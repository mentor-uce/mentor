"""Build/run the real backend Compose model in an owned, disposable project.

Requires Python3, Docker Compose >=2.35, Node and npm ci in scripts/socketio-check.
Never reads a developer's service .env; all secrets are generated for this test.
"""
import argparse
import json
import os
from pathlib import Path
import secrets
import subprocess
import sys
import tempfile
import time
import urllib.error
import urllib.request

sys.path.insert(0, str(Path(__file__).resolve().parent))
from verify_keycloak import login, request_json


def require(condition, message):
    if not condition:
        raise AssertionError(message)


def run(arguments, environment=None):
    result = subprocess.run(arguments, env=environment, capture_output=True, text=True)
    if result.returncode:
        raise RuntimeError("Command failed: " + " ".join(arguments[:3]) + "\n" + result.stderr[-1600:])
    return result.stdout.strip()


def wait_until(predicate, label, timeout=180):
    deadline = time.monotonic() + timeout
    while time.monotonic() < deadline:
        try:
            if predicate():
                return
        except (OSError, urllib.error.URLError, ValueError):
            pass
        time.sleep(1)
    raise AssertionError("Timed out waiting for " + label)


def verify(repository, image=None):
    suffix = secrets.token_hex(8)
    project = "mentor-smoke-" + suffix
    owned_image = None if image else "mentor-backend-smoke:" + suffix
    selected_image = image or owned_image
    values = {
        "POSTGRES_USER": "mentor_smoke", "POSTGRES_DB": "mentor_smoke", "POSTGRES_PASSWORD": secrets.token_hex(24),
        "MONGO_USER": "mentor_smoke", "MONGO_DB": "mentor_smoke", "MONGO_PASSWORD": secrets.token_hex(24),
        "MONGO_AUTH_DATABASE": "admin", "REDIS_PASSWORD": secrets.token_hex(24),
        "KEYCLOAK_ADMIN": "smoke_admin", "KEYCLOAK_ADMIN_PASSWORD": secrets.token_hex(24), "KEYCLOAK_REALM": "mentor",
        "MENTOR_TEST_STUDENT_PASSWORD": secrets.token_hex(24), "MENTOR_TEST_TUTOR_PASSWORD": secrets.token_hex(24),
        "MENTOR_TEST_ADMIN_PASSWORD": secrets.token_hex(24), "KEYCLOAK_API_AUDIENCE": "mentor-api",
        "SPRING_PROFILES_ACTIVE": "local", "SOCKET_IO_ENABLED": "true", "SOCKET_IO_HOST": "0.0.0.0", "SOCKET_IO_PORT": "9092",
        "PAYMENT_PROVIDER": "SIMULATED", "VIDEO_PROVIDER": "DUMMY", "CORS_ALLOWED_ORIGINS": "http://localhost:5173",
    }
    # Keep Docker daemon/context discovery, but prevent caller configuration from
    # overriding fixture values or injecting additional Compose models/profiles.
    environment = {key: value for key, value in os.environ.items()
                   if key not in values and not key.startswith(("COMPOSE_", "SPRING_", "KEYCLOAK_", "POSTGRES_", "MONGO_", "REDIS_", "SOCKET_IO_"))}
    with tempfile.TemporaryDirectory(prefix=project + "-") as directory:
        temporary = Path(directory)
        env_file = temporary / "fixture.env"
        env_file.write_text("\n".join(key + "=" + value for key, value in values.items()), encoding="utf-8")
        base = ["docker", "compose", "--project-name", project, "--env-file", str(env_file)]
        raw = run(base + ["--file", str(repository / "docker-compose.yml"), "--profile", "app",
                          "config", "--no-env-resolution", "--format", "json"], environment)
        config = json.loads(raw)
        require(not run(["docker", "ps", "-aq", "--filter", "label=com.docker.compose.project=" + project]), "Project name already in use")
        require(not run(["docker", "volume", "ls", "-q", "--filter", "label=com.docker.compose.project=" + project]), "Project volumes already exist")
        for resources in (config.get("volumes", {}), config.get("networks", {})):
            for resource in resources.values():
                require(not resource.get("external"), "External resources cannot be used by this verifier")
                require(resource.get("name", "").startswith(project + "_"), "Resource name is not owned by fixture")
        services = config["services"] = {key: config["services"][key] for key in ("postgres", "mongo", "redis", "keycloak", "backend")}
        for service in services.values():
            service.pop("env_file", None)
            service.pop("profiles", None)
            service.pop("container_name", None)
            service["restart"] = "no"
            service["ports"] = []
        services["keycloak"]["ports"] = [{"target": 8080, "host_ip": "127.0.0.1", "published": "0", "protocol": "tcp"}]
        services["backend"]["ports"] = [{"target": port, "host_ip": "127.0.0.1", "published": "0", "protocol": "tcp"} for port in (8080, 9092)]
        services["backend"]["image"] = selected_image
        if image:
            services["backend"].pop("build", None)
        path = temporary / "compose.json"
        path.write_text(json.dumps(config), encoding="utf-8")
        compose = base + ["--file", str(path)]
        cleanup = False
        try:
            if not image:
                print("Building backend/Dockerfile", flush=True)
                run(compose + ["build", "backend"], environment)
                print("Backend image built", flush=True)
            cleanup = True
            run(compose + ["up", "-d", "postgres", "mongo", "redis", "keycloak"], environment)

            def container(service):
                identifier = run(compose + ["ps", "-q", service], environment)
                require(identifier and "\n" not in identifier, "Expected one owned " + service + " container")
                actual = json.loads(run(["docker", "inspect", identifier]))[0]
                require(actual["Config"]["Labels"].get("com.docker.compose.project") == project, "Container is not owned")
                return identifier

            def mapped_port(service, port):
                return run(compose + ["port", service, str(port)], environment).rsplit(":", 1)[1]

            keycloak = "http://localhost:" + mapped_port("keycloak", 8080) + "/realms/mentor"
            wait_until(lambda: request_json(keycloak + "/.well-known/openid-configuration").get("issuer") == keycloak, "imported Compose Keycloak realm")
            print("Compose Keycloak realm ready", flush=True)
            services["backend"]["environment"]["SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI"] = keycloak
            services["backend"]["environment"]["SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_JWK_SET_URI"] = "http://keycloak:8080/realms/mentor/protocol/openid-connect/certs"
            path.write_text(json.dumps(config), encoding="utf-8")
            run(compose + ["up", "-d", "--no-build", "backend"], environment)
            backend_id = container("backend")
            api = "http://127.0.0.1:" + mapped_port("backend", 8080)
            wait_until(lambda: request_json(api + "/actuator/health").get("status") == "UP", "backend HTTP health UP")
            health = request_json(api + "/actuator/health")
            require("components" not in health and "details" not in health, "Public health must hide internals")
            print("Backend HTTP health UP", flush=True)
            wait_until(lambda: json.loads(run(["docker", "inspect", backend_id]))[0]["State"]["Health"]["Status"] == "healthy", "actual Compose healthcheck")
            print("Compose healthcheck healthy; verifying identity and provider contracts", flush=True)
            require(run(["docker", "exec", backend_id, "id", "-u"]) != "0", "Backend must run as nonroot")
            run(["docker", "exec", backend_id, "sh", "-c", "test ! -f /app/.env"])
            require(request_json(api + "/api/v1/hello")["message"] == "Mentor backend is running", "Hello contract changed")
            for route in ("/api/v1/auth/me", "/api/v1/payments/status"):
                try:
                    request_json(api + route)
                except urllib.error.HTTPError as error:
                    require(error.code == 401, "Expected anonymous401")
                else:
                    raise AssertionError("Anonymous request was accepted")
            student = None
            for role in ("STUDENT", "TUTOR", "ADMIN"):
                token = login(keycloak, role.lower() + ".local", values["MENTOR_TEST_" + role + "_PASSWORD"])
                me = request_json(api + "/api/v1/auth/me", bearer=token)
                require(me["username"] == role.lower() + ".local", "Unexpected authenticated identity")
                require(me["roles"] == (["STUDENT", "TUTOR"] if role == "TUTOR" else [role]), "Unexpected roles")
                if role == "STUDENT":
                    student = token
            require(request_json(api + "/api/v1/payments/status", bearer=student) ==
                    {"provider": "SIMULATED", "testMode": True, "chargesEnabled": False}, "Payment scaffold contract changed")
            payload = json.dumps({"url": "http://127.0.0.1:" + mapped_port("backend", 9092) + "/mentor", "token": student})
            subprocess.run(["node", str(repository / "backend/scripts/socketio-check/check.mjs")],
                           input=payload, text=True, check=True, timeout=20, env=environment)
            print(json.dumps({"dockerfile": "built" if owned_image else "prebuilt image verified", "health": "UP", "composeHealthcheck": "healthy",
                              "nonroot": True, "keycloakIdentities": 3, "anonymous": "401", "payments": "SIMULATED", "socketio": "passed"}), flush=True)
        finally:
            if cleanup:
                run(compose + ["down", "--volumes", "--remove-orphans"], environment)
            if owned_image:
                subprocess.run(["docker", "image", "rm", owned_image], capture_output=True)


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--repository", type=Path, default=Path(__file__).resolve().parents[2])
    parser.add_argument("--image", help="Use the image already built from this checkout in the same CI job")
    args = parser.parse_args()
    verify(args.repository.resolve(), args.image)
