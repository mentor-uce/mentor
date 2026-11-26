"""Disposable Keycloak realm/PKCE verification. Requires Python 3 and Docker."""
import argparse
import base64
import hashlib
import http.cookiejar
import json
import os
from pathlib import Path
import secrets
import subprocess
import tempfile
import time
import urllib.error
import urllib.parse
import urllib.request
from html.parser import HTMLParser


def docker(*args):
    result = subprocess.run(["docker", *args], capture_output=True, text=True, check=True)
    return result.stdout.strip()


def request_json(url, data=None, bearer=None):
    headers = {"Accept": "application/json"}
    if bearer:
        headers["Authorization"] = "Bearer " + bearer
    body = urllib.parse.urlencode(data).encode() if data is not None else None
    with urllib.request.urlopen(urllib.request.Request(url, body, headers), timeout=15) as response:
        return json.load(response)


class LoginForm(HTMLParser):
    def __init__(self):
        super().__init__()
        self.action = None
        self.fields = {}

    def handle_starttag(self, tag, attrs):
        attrs = dict(attrs)
        if tag == "form" and attrs.get("id") == "kc-form-login":
            self.action = attrs["action"]
        if tag == "input" and attrs.get("type") == "hidden" and attrs.get("name"):
            self.fields[attrs["name"]] = attrs.get("value", "")


class Callback(Exception):
    def __init__(self, url):
        self.url = url


class CatchCallback(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, req, fp, code, msg, headers, newurl):
        if newurl.startswith("http://localhost:5173/callback"):
            raise Callback(newurl)
        return super().redirect_request(req, fp, code, msg, headers, newurl)


class LoopbackCookiePolicy(http.cookiejar.DefaultCookiePolicy):
    """Match browsers' trusted-localhost cookie behavior only for this fixture."""
    def __init__(self, base):
        super().__init__()
        self.fixture_origin = urllib.parse.urlsplit(base).netloc

    def return_ok_secure(self, cookie, request):
        url = urllib.parse.urlsplit(request.full_url)
        if url.scheme == "http" and url.hostname == "localhost" and url.netloc == self.fixture_origin:
            return True
        return super().return_ok_secure(cookie, request)


def login(base, username, password):
    verifier = secrets.token_urlsafe(48)
    challenge = base64.urlsafe_b64encode(hashlib.sha256(verifier.encode()).digest()).rstrip(b"=").decode()
    state = secrets.token_urlsafe(24)
    redirect = "http://localhost:5173/callback"
    params = {"client_id": "mentor-web", "response_type": "code", "scope": "openid profile email",
              "redirect_uri": redirect, "state": state, "code_challenge": challenge,
              "code_challenge_method": "S256"}
    jar = http.cookiejar.CookieJar(policy=LoopbackCookiePolicy(base))
    opener = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(jar), CatchCallback())
    with opener.open(base + "/protocol/openid-connect/auth?" + urllib.parse.urlencode(params), timeout=15) as response:
        form = LoginForm()
        form.feed(response.read().decode())
    assert form.action, "Expected Keycloak login form"
    fields = dict(form.fields, username=username, password=password)
    try:
        opener.open(form.action, urllib.parse.urlencode(fields).encode(), timeout=15)
    except urllib.error.HTTPError as error:
        import re
        text = re.sub(r"<[^>]+>", " ", error.read().decode())
        text = " ".join(text.split()).replace(password, "[REDACTED]")
        raise AssertionError("Keycloak login rejected: " + text[-1200:]) from None
    except Callback as callback:
        query = urllib.parse.parse_qs(urllib.parse.urlparse(callback.url).query)
    else:
        raise AssertionError("PKCE login did not return an authorization code")
    assert query.get("state") == [state], "OIDC state mismatch"
    token = request_json(base + "/protocol/openid-connect/token", {
        "grant_type": "authorization_code", "client_id": "mentor-web", "code": query["code"][0],
        "redirect_uri": redirect, "code_verifier": verifier})
    return token["access_token"]


def claims(token):
    payload = token.split(".")[1]
    return json.loads(base64.urlsafe_b64decode(payload + "=" * (-len(payload) % 4)))


def verify(repository, backend_jar=None, expect_me=False):
    realm = Path(repository) / "backend/docker/keycloak/mentor-realm.json"
    assert realm.is_file(), "Missing mentor-realm.json"
    configuration = json.loads(realm.read_text(encoding="utf-8"))
    web = next(c for c in configuration["clients"] if c["clientId"] == "mentor-web")
    assert web["publicClient"] and web["standardFlowEnabled"]
    assert not web["directAccessGrantsEnabled"] and not web["implicitFlowEnabled"] and not web["serviceAccountsEnabled"]
    assert web["attributes"]["pkce.code.challenge.method"] == "S256"
    name = "mentor-keycloak-check-" + secrets.token_hex(4)
    owned_container_id = None
    password = secrets.token_hex(24)
    user_passwords = {role: secrets.token_hex(24) for role in ("STUDENT", "TUTOR", "ADMIN")}
    try:
        with tempfile.TemporaryDirectory(prefix="mentor-keycloak-") as directory:
            env_file = Path(directory) / "keycloak.env"
            env_file.write_text("KC_BOOTSTRAP_ADMIN_USERNAME=admin\nKC_BOOTSTRAP_ADMIN_PASSWORD=" + password + "\n" +
                                "\n".join("MENTOR_TEST_" + role + "_PASSWORD=" + value for role, value in user_passwords.items()), encoding="utf-8")
            owned_container_id = docker("run", "-d", "--rm", "--name", name, "--env-file", str(env_file),
                   "-p", "127.0.0.1::8080", "-v", str(realm.resolve()) + ":/opt/keycloak/data/import/mentor-realm.json:ro",
                   "quay.io/keycloak/keycloak:26.0", "start-dev", "--import-realm", "--hostname-strict=false")
            port = docker("port", name, "8080/tcp").split(":")[-1]
            root = "http://localhost:" + port
            base = root + "/realms/mentor"
            for _ in range(150):
                try:
                    discovery = request_json(base + "/.well-known/openid-configuration")
                    break
                except (OSError, urllib.error.URLError):
                    time.sleep(1)
            else:
                raise AssertionError("Keycloak failed to import/start")
            assert discovery["issuer"] == base
            admin = request_json(root + "/realms/master/protocol/openid-connect/token", {
                "client_id": "admin-cli", "grant_type": "password", "username": "admin", "password": password})["access_token"]
            clients = request_json(root + "/admin/realms/mentor/clients?clientId=mentor-web", bearer=admin)
            actual = request_json(root + "/admin/realms/mentor/clients/" + clients[0]["id"], bearer=admin)
            assert actual["publicClient"] and actual["attributes"]["pkce.code.challenge.method"] == "S256"
            assert not actual["directAccessGrantsEnabled"] and not actual["implicitFlowEnabled"]
            issued_tokens = {}
            for role, secret in user_passwords.items():
                username = role.lower() + ".local"
                token = login(base, username, secret)
                issued_tokens[role] = token
                payload = claims(token)
                assert payload.get('sub'), 'Access token is missing sub'
                assert payload.get('exp'), 'Access token is missing exp'
                audiences = payload["aud"]
                if isinstance(audiences, str):
                    audiences = [audiences]
                assert payload["iss"] == base and "mentor-api" in audiences
                expected = {"STUDENT", "TUTOR"} if role == "TUTOR" else {role}
                actual_roles = set(payload["realm_access"]["roles"]) & {"STUDENT", "TUTOR", "ADMIN"}
                assert actual_roles == expected, "Unexpected application roles"
                assert payload["preferred_username"] == username
            try:
                request_json(base + "/protocol/openid-connect/token", {
                    "client_id": "mentor-web", "grant_type": "password", "username": "student.local",
                    "password": user_passwords["STUDENT"]})
            except urllib.error.HTTPError as error:
                assert error.code == 400
            else:
                raise AssertionError("Public frontend password grant unexpectedly enabled")
            params = {"client_id": "mentor-web", "response_type": "code", "redirect_uri": "http://localhost:5173/callback"}
            try:
                urllib.request.build_opener(CatchCallback()).open(base + "/protocol/openid-connect/auth?" + urllib.parse.urlencode(params), timeout=10)
            except urllib.error.HTTPError as error:
                assert error.code == 400
            except Callback as callback:
                query = urllib.parse.parse_qs(urllib.parse.urlparse(callback.url).query)
                assert query.get("error") == ["invalid_request"] and "code" not in query
            else:
                raise AssertionError("Authorization without S256 unexpectedly accepted")
            if backend_jar:
                verify_backend(backend_jar, base, issued_tokens, expect_me)
            return {"realm": "mentor", "client": "mentor-web", "pkceS256": "passed", "testUserLogins": 3,
                    "rolesAndAudience": "passed", "passwordGrant": "rejected", "missingPkce": "rejected"}
    finally:
        if owned_container_id:
            subprocess.run(["docker", "rm", "-f", owned_container_id], capture_output=True)


def verify_backend(backend_jar, issuer, tokens, expect_me=False):
    """Verify production JWT filters against actual Keycloak-issued access tokens."""
    import socket
    suffix = secrets.token_hex(4)
    names = {service: "mentor-auth-check-" + service + "-" + suffix for service in ("pg", "mongo", "redis")}
    process = None
    owned_container_ids = []
    password = secrets.token_hex(24)
    manager = tempfile.TemporaryDirectory(prefix="mentor-auth-")
    try:
        directory = Path(manager.name)
        env_file = directory / "services.env"
        env_file.write_text("\n".join(("POSTGRES_USER=mentor_check", "POSTGRES_DB=mentor_check",
            "POSTGRES_PASSWORD=" + password, "MONGO_INITDB_ROOT_USERNAME=mentor_check",
            "MONGO_INITDB_ROOT_PASSWORD=" + password, "REDIS_PASSWORD=" + password)), encoding="utf-8")
        for service, image, port, command in (
            ("pg", "postgres:16-alpine", 5432, []),
            ("mongo", "mongo:7", 27017, []),
            ("redis", "redis:7-alpine", 6379, ["sh", "-c", 'exec redis-server --requirepass "$REDIS_PASSWORD" --save "" --appendonly no'])):
            owned_container_ids.append(docker("run", "-d", "--rm", "--name", names[service], "--env-file", str(env_file),
                   "-p", "127.0.0.1::" + str(port), image, *command))
        ports = {service: docker("port", names[service], str(port) + "/tcp").split(":")[-1]
                 for service, port in (("pg", 5432), ("mongo", 27017), ("redis", 6379))}
        ready = False
        for _ in range(80):
            pg = subprocess.run(["docker", "exec", names["pg"], "pg_isready", "-U", "mentor_check", "-d", "mentor_check"], capture_output=True)
            mongo = subprocess.run(["docker", "exec", names["mongo"], "mongosh", "--quiet", "--eval",
                "const a=db.getSiblingDB('admin');a.auth(process.env.MONGO_INITDB_ROOT_USERNAME,process.env.MONGO_INITDB_ROOT_PASSWORD);if(a.runCommand({ping:1}).ok!==1)quit(1)"], capture_output=True)
            if pg.returncode == 0 and mongo.returncode == 0:
                ready = True
                break
            time.sleep(.5)
        assert ready, "Temporary auth-test databases did not start"
        with socket.socket() as listener:
            listener.bind(("127.0.0.1", 0))
            api_port = listener.getsockname()[1]
        environment = {key: value for key, value in os.environ.items()
                       if not key.startswith(("SPRING_", "MONGO_", "POSTGRES_", "REDIS_", "KEYCLOAK_"))}
        environment.update({"SERVER_PORT": str(api_port), "SERVER_ADDRESS": "127.0.0.1",
            "POSTGRES_HOST": "127.0.0.1", "POSTGRES_PORT": ports["pg"], "POSTGRES_DB": "mentor_check",
            "POSTGRES_USER": "mentor_check", "POSTGRES_PASSWORD": password,
            "MONGO_HOST": "127.0.0.1", "MONGO_PORT": ports["mongo"], "MONGO_DB": "mentor_check",
            "MONGO_USER": "mentor_check", "MONGO_PASSWORD": password,
            "REDIS_HOST": "127.0.0.1", "REDIS_PORT": ports["redis"], "REDIS_PASSWORD": password,
            "KEYCLOAK_ISSUER_URI": issuer, "KEYCLOAK_JWK_SET_URI": issuer + "/protocol/openid-connect/certs",
            "KEYCLOAK_API_AUDIENCE": "mentor-api"})
        java = str(Path(environment["JAVA_HOME"]) / "bin" / ("java.exe" if os.name == "nt" else "java")) if environment.get("JAVA_HOME") else "java"
        with (directory / "backend.log").open("w", encoding="utf-8") as log:
            process = subprocess.Popen([java, "-jar", str(Path(backend_jar).resolve())], cwd=directory,
                                       env=environment, stdout=log, stderr=subprocess.STDOUT)
            root = "http://127.0.0.1:" + str(api_port)
            for _ in range(100):
                if process.poll() is not None:
                    raise AssertionError("Backend exited before real-Keycloak token verification")
                try:
                    if request_json(root + "/actuator/health")["status"] == "UP":
                        break
                except (OSError, urllib.error.URLError):
                    time.sleep(.5)
            else:
                raise AssertionError("Backend health did not become UP")

            def api(token=None):
                headers = {"Accept": "application/json"}
                if token:
                    headers["Authorization"] = "Bearer " + token
                request = urllib.request.Request(root + "/api/v1/auth/me", headers=headers)
                try:
                    with urllib.request.urlopen(request, timeout=10) as response:
                        return response.status, json.load(response), response.headers
                except urllib.error.HTTPError as error:
                    return error.code, {}, error.headers

            assert api()[0] == 401 and api("invalid-token")[0] == 401
            for role, token in tokens.items():
                status, payload, headers = api(token)
                assert status == (200 if expect_me else 404), f"Real Keycloak token did not authenticate: status={status}, challenge={headers.get('WWW-Authenticate')}"
                assert "Set-Cookie" not in headers
                if expect_me:
                    actual = claims(token)
                    assert payload["id"] == actual["sub"] and payload["username"] == role.lower() + ".local"
                    expected = ["STUDENT", "TUTOR"] if role == "TUTOR" else [role]
                    assert payload["roles"] == expected
                    assert set(payload) == {"id", "username", "email", "roles"}
            if expect_me:
                print("Real Keycloak JWTs verified by /auth/me for STUDENT, TUTOR and ADMIN", flush=True)
            else:
                print("Real Keycloak JWTs passed signature/issuer/audience validation; protected route still unmapped", flush=True)
    finally:
        if process and process.poll() is None:
            process.terminate()
            try:
                process.wait(timeout=10)
            except subprocess.TimeoutExpired:
                process.kill()
                process.wait()
        for name in owned_container_ids:
            subprocess.run(["docker", "rm", "-f", name], capture_output=True)
        manager.cleanup()


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--repository", type=Path, default=Path(__file__).resolve().parents[2])
    parser.add_argument("--backend-jar", type=Path)
    parser.add_argument("--expect-me", action="store_true")
    args = parser.parse_args()
    print(json.dumps(verify(args.repository, args.backend_jar, args.expect_me), indent=2))
