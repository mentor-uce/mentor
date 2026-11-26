# Mentor – Backend

Mentor API: Spring Boot (Java 21) modular monolith with DDD.

Part of **Mentor – Learn and teach among peers**, a peer-tutoring web platform for students of Universidad Central del Ecuador.

This folder is part of the `mentor` monorepo: the web app and landing page are in `frontend/web` and `frontend/landing`; Docker Compose is in this folder; the documentation is in Confluence.

**Owner:** Alquinga Cristopher (@Cristopher2912)

- Web app and landing page: `frontend/web` and `frontend/landing` of this monorepo
- **Documentation (architecture, diagrams, workflow): [Confluence – MENTOR](https://kevinteno11.atlassian.net/wiki/spaces/MEN/pages/65927)**

## Current implementation

- Java 21 and Spring Boot 4.2.0
- Spring MVC with an embedded HTTP server
- Maven 3.10.1 through the checked-in Maven Wrapper
- Actuator health, GET `/api/v1/hello`, OpenAPI and Swagger UI
- Configurable API CORS and optional local-profile `.env` import
- Spring Data JPA and PostgreSQL connection configured through environment variables
- Flyway startup migrations with SQL checksum validation
- Spring Data MongoDB with an authenticated connection and health integration
- HTTP smoke and startup connectivity tests against temporary PostgreSQL containers

Healthy operation now requires reachable PostgreSQL, MongoDB and Redis services and their credentials. Keycloak realm and JWT validation are implemented; payment integrations remain planned.
Business modules and external integrations remain in their respective Jira tasks. The root `/` remains unmapped; use `/api/v1/hello` for an API check.

## Target tech stack

- Java 21 + Spring Boot
- Spring Security as an OAuth 2.0 resource server (validates the Keycloak JWT and applies roles)
- REST /api/v1 documented with OpenAPI (springdoc) and GraphQL (Spring for GraphQL)
- Socket.IO for chat and live status (compatibility proof in MEN-23)
- Spring Data JPA (PostgreSQL/Supabase), Spring Data MongoDB, Spring Data Redis and a Qdrant adapter
- Spring Boot Actuator (`/actuator/health` is used by the Docker health check)
- JUnit Jupiter and Mockito for tests (versions managed by Spring Boot)
- Maven as the build tool

## Existing package structure

The application root package is `com.mentor`. The following module packages
each contain `presentation`, `application`, `domain` and `infrastructure`:

`auth`, `users`, `services`, `schedules`, `bookings`, `sessions`, `payments`,
`messaging`, `video`, `notifications`, `reviews`, `admin` and `shared`.

For example:

```text
src/main/java/com/mentor/bookings/
  presentation/package-info.java
  application/package-info.java
  domain/package-info.java
  infrastructure/package-info.java
```

These packages contain documentation only. `package-info.java` keeps the empty
layers versioned without adding controllers, entities, use cases or repositories.
The bootstrap class and its context test use the same root package so Spring can
discover future module components without configuring a second scan root.

## Planned modules

Auth, Users, Services, Schedules, Bookings, Sessions, Payments, Messaging, Video, Reviews, Notifications and Admin.

## Getting started

Run the commands from `backend` of the monorepo, where `pom.xml` lives.

### Prerequisites

- JDK 21; `java -version` must report version 21.
- `JAVA_HOME` pointing to that JDK and its `bin` directory on `PATH`.
- Internet access on the first build to download Maven and dependencies.

Maven does not need a separate installation: the Maven Wrapper downloads its
pinned version on first use.

### Windows (PowerShell)

```powershell
.\mvnw.cmd -v                  # confirm Maven and Java versions
.\mvnw.cmd clean verify        # compile, run tests and package the application
.\mvnw.cmd spring-boot:run '-Dspring-boot.run.profiles=local'  # loads local .env
```

### Linux / macOS

```bash
./mvnw -v
./mvnw clean verify
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

### Run the packaged application

After a successful build:

```bash
java -jar target/mentor-backend-0.0.1-SNAPSHOT.jar --spring.profiles.active=local
```

The default port is `8080`. Set `SERVER_PORT` or pass `--server.port=8082` to
change it. Stop the application with `Ctrl+C`.

For local runs, copy `.env.example` to `.env` if it does not already exist,
set `POSTGRES_PASSWORD` and activate the `local` profile. This imports the
optional `.env` from the process working directory. Use `KEY=value` syntax
without shell quoting or `export`. Real process environment variables take precedence.
Start PostgreSQL and MongoDB before running the API; see MEN-16 and MEN-18 below.
Never commit `.env` files or real credentials.

The existing `Dockerfile` builds the application with Maven and runs the JAR on
port 8080. Its Compose health check uses `/actuator/health`.

## Repository setup (MEN-12)

The current monorepo separates `backend/` from `frontend/web/` and
`frontend/landing/`. Maven Wrapper, `pom.xml`, Java sources, the Dockerfile,
Compose and `.env.example` are inside `backend/`, not the repository root.

The Jira task names `feature/backend-core` as the base work branch. From the
monorepo root, update it without rewriting history:

```bash
git fetch origin
git switch feature/backend-core
git merge --ff-only origin/develop
```

For a fresh clone, first create the local tracking branch with
`git switch --track origin/feature/backend-core`. If fast-forward fails, inspect
the divergent changes; do not reset or force-push the branch.

Create `backend/.env` from `backend/.env.example` only if it does not already
exist. Set the four required local passwords (`POSTGRES_PASSWORD`,
`MONGO_PASSWORD`, `REDIS_PASSWORD`, `KEYCLOAK_ADMIN_PASSWORD`) outside Git.
From `backend/`, validate all Compose profiles without starting containers:

```bash
docker compose --profile app --profile automation config --quiet
```

The template documents the variables used by Compose and direct local runs. `.env` and `.env.local` are ignored. Validation of this setup
does not establish connectivity to databases, Keycloak or payment/video
providers; those integrations remain in their implementation tasks.

## API checks and documentation (MEN-15)

Run from `backend/`. Copy `.env.example` to `.env` if needed and activate the
`local` profile as shown above. `SERVER_PORT` defaults to 8080;
`CORS_ALLOWED_ORIGINS` is a comma-separated list of exact frontend origins,
for example `http://localhost:5173,http://127.0.0.1:5173`.

| URL | Expected result |
| --- | --- |
| `http://localhost:8080/api/v1/hello` | HTTP 200 and `{"message":"Mentor backend is running"}`. |
| `http://localhost:8080/actuator/health` | HTTP 200 and `status: UP`; no component details or secrets. Health group names may also be present. |
| `http://localhost:8080/v3/api-docs` | OpenAPI JSON with Mentor API metadata and `/api/v1/hello`. |
| `http://localhost:8080/swagger-ui/index.html` | Swagger UI served by this application. `/swagger-ui.html` redirects to it. |

Only the health actuator is exposed. API CORS accepts configured origins and
rejects unlisted origins; wildcard origins are not accepted. Bearer-token
headers are allowed without enabling cross-origin cookie credentials.
Socket.IO will have its own origin policy when implemented.

Health and documentation are public for this bootstrap task. The Keycloak
security task must apply the agreed environment/role policy for documentation
and business routes; it must not expose additional management endpoints.
Health now includes PostgreSQL and MongoDB connectivity in its aggregate status, while component details stay hidden. Other provider connectivity remains outside this task.

```powershell
Invoke-RestMethod http://localhost:8080/actuator/health
Invoke-RestMethod http://localhost:8080/api/v1/hello
```

With Docker running, run `./mvnw verify` (or `.\mvnw.cmd verify` on Windows) for PostgreSQL startup and HTTP
smoke tests: health, restricted management exposure, hello contract, OpenAPI,
Swagger UI, allowed/blocked origins and CORS preflight.

## PostgreSQL connection (MEN-16)

Spring Data JPA uses the PostgreSQL JDBC driver and HikariCP; dependency
versions are managed by Spring Boot. Set these values in your ignored `.env`:

| Variable | Purpose / default |
| --- | --- |
| `POSTGRES_HOST` | Database host for direct JVM runs; `localhost`. |
| `POSTGRES_PORT` | Database port for direct JVM runs; `5432`. |
| `POSTGRES_DB` | Database name; `mentor`. |
| `POSTGRES_USER` | Database user; `mentor`. |
| `POSTGRES_PASSWORD` | Required password you set locally; no supplied secret. |
| `SPRING_DATASOURCE_URL` | Optional full JDBC URL override, including hosted PostgreSQL options. |
| `SPRING_DATASOURCE_USERNAME` / `SPRING_DATASOURCE_PASSWORD` | Optional matching credential overrides. |

For local Compose, also fill the other required password placeholders in
`.env` because Compose validates the full file even when starting one service.
From `backend/`:

```bash
docker compose up -d postgres mongo redis
docker compose ps postgres mongo redis
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

On PowerShell, use `.\mvnw.cmd` instead of `./mvnw`. With PostgreSQL ready,
the log should show the connection pool and JPA EntityManagerFactory starting;
`/actuator/health` should return HTTP 200 with `status: UP`.
Compose passes `SPRING_DATASOURCE_*` for its internal `postgres:5432` address;
do not use `localhost` for the database host inside the backend container.
For hosted databases, set the provider JDBC URL and TLS options plus credentials
through the environment. This task does not provision a Supabase instance.

Hibernate automatic schema creation/update is disabled (`ddl-auto=none`),
and Open Session in View is disabled. No entities or business tables are added. With incorrect credentials
or an unreachable database, startup fails; do not treat a disconnected API as healthy.

`mvn verify` requires Docker: Testcontainers starts PostgreSQL 16 with ephemeral
credentials and random ports, runs `SELECT 1`, checks JPA initialization and
runs the existing HTTP checks against the connected application. It does not
use your local `.env`, database or credentials; containers are stopped afterward.
CI runners already provide Docker. Reference:
[Spring Boot SQL configuration](https://docs.spring.io/spring-boot/reference/data/sql.html)
and [Testcontainers service connections](https://docs.spring.io/spring-boot/reference/testing/testcontainers.html).

## Flyway migrations (MEN-17)

Flyway runs automatically on the application DataSource before JPA initialization.
Spring Boot manages Flyway's version; the PostgreSQL database module is included.
No additional credentials or manual SQL execution are needed for migrations.

Production migration files live in `src/main/resources/db/migration/` and follow
`V<version>__<description>.sql`. The first file, `V1__init.sql`, intentionally
executes only `SELECT 1`: it verifies the migration pipeline without inventing
business tables. Flyway creates `flyway_schema_history`, records version 1 as
successful and stores its checksum. Future schema changes get new versions
(`V2__...sql`, etc.); never edit a migration already applied to a shared database.

After starting the API, check the Flyway startup log and, if needed, run this
read-only query against the configured application database:

```sql
SELECT version, script, success, checksum
FROM flyway_schema_history
ORDER BY installed_rank;
```

Expect one successful `V1__init.sql` record. Restarting against the same database
validates it without applying V1 again. A checksum mismatch stops startup.
Automatic baselining is disabled: an existing schema with application objects
and no Flyway history requires a reviewed adoption plan; do not enable baseline
or delete data just to make startup pass. Flyway clean is disabled, and Hibernate
keeps `ddl-auto=none`. The existing Compose initialization creates the pgvector
extension and separate Keycloak/n8n databases; Flyway uses only the application's
configured database, not those other databases.

`mvn verify` includes a real PostgreSQL startup assertion that V1 was successfully
recorded, alongside the connection and HTTP tests. Docker is required.
Reference: [Spring Boot migration initialization](https://docs.spring.io/spring-boot/how-to/data-initialization.html).

## MongoDB connection and ping (MEN-18)

Spring Data MongoDB uses the synchronous driver, with versions managed by Spring
Boot. Connection properties in Boot 4 use `spring.mongodb.*`; the former
`SPRING_DATA_MONGODB_URI` setting is not used. For direct local runs, set these
values in the ignored `.env` and activate the `local` Spring profile:

| Variable | Purpose / default |
| --- | --- |
| `MONGO_HOST` | Server host; `localhost`. Compose overrides it with `mongo`. |
| `MONGO_PORT` | Server port; `27017`. |
| `MONGO_DB` | Application database; `mentor`. |
| `MONGO_USER` | Login user; `mentor`. Local Compose creates this root test user. |
| `MONGO_PASSWORD` | Required local password; no supplied secret. |
| `MONGO_AUTH_DATABASE` | Database holding the user's credentials; `admin` for local Compose. |

Separate username/password properties accept special characters without URI
encoding. For hosted MongoDB, set `SPRING_MONGODB_URI` in the actual process
environment. In the imported `.env` file use the lowercase property
`spring.mongodb.uri=...` instead; the example shows this optional setting.
URI credentials must be percent-encoded. `MONGO_DB` still determines the
application database, even when a URI includes a database name. Supply the
provider's TLS and replica-set options in its URI. Do not put real URIs in Git.

From `backend/`, after filling the required local passwords:

```bash
docker compose up -d postgres mongo redis
docker compose ps postgres mongo redis
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

On PowerShell use `.\mvnw.cmd`. A successful `/actuator/health` response is
HTTP 200 with `status: UP` and no component details. Invalid MongoDB credentials
or an unavailable MongoDB server make the health check return HTTP 503/DOWN;
MongoDB's lazy client does not guarantee startup will fail immediately.
The Compose MongoDB health check authenticates before running `ping`.

`mvn verify` starts temporary PostgreSQL and MongoDB 7 containers. The MongoDB
test executes `{ping: 1}` through the application's configured `MongoTemplate`
and checks `ok: 1`. Existing SQL migration and HTTP tests still run. No business
collections, document models or MongoDB repositories are added by this task.
Use separate limited users for hosted environments; the Compose root user is
for the local development setup.

Reference: [Spring Boot MongoDB configuration](https://docs.spring.io/spring-boot/reference/data/nosql.html).

## Redis cache connectivity (MEN-19)

Spring Data Redis uses Lettuce and `spring.data.redis.*`. In the local-profile
`.env`, set `REDIS_HOST` (localhost), `REDIS_PORT` (6379) and `REDIS_PASSWORD`.
Compose passes its internal hostname and password through `SPRING_DATA_REDIS_*`.
Real process variables override file values. No secret is supplied in Git.

Start Redis alongside PostgreSQL and MongoDB before checking health. The aggregate
health response includes Redis; authentication/connectivity failures return
HTTP 503/DOWN without component details. Command and connection timeouts are 2s.
Redis client creation is lazy and is not a promise that startup checks credentials.

The real-container test uses the configured `StringRedisTemplate`: writes a
unique key with a one-minute TTL, reads the exact value, checks finite expiry
and PONG, then removes only its key. Business cache policies remain with their
modules. Redis repositories are disabled. Local Compose disables snapshots and
append-only persistence; Redis is a disposable cache, not business storage. Its /data directory uses tmpfs, so recreating the container cannot reload an old dump. Existing redis-data volumes are left untouched and unmounted.

Reference: [Spring Boot Redis](https://docs.spring.io/spring-boot/reference/data/nosql.html).

## Local Keycloak realm (MEN-20)

The local-only realm definition is `docker/keycloak/mentor-realm.json`, mounted
read-only by Compose and imported at startup. Set distinct generated passwords
in `.env`: `MENTOR_TEST_STUDENT_PASSWORD`, `MENTOR_TEST_TUTOR_PASSWORD` and
`MENTOR_TEST_ADMIN_PASSWORD`, as well as the separate Keycloak bootstrap-admin
password. No test-user password is stored in the realm file. Keycloak resolves
the environment placeholders on import.

The `mentor` realm has `STUDENT`, `TUTOR` and `ADMIN` roles. `TUTOR` includes
`STUDENT`. Seed accounts `student.local`, `tutor.local` and `admin.local` use
synthetic example.invalid addresses. `admin.local` is a Mentor application role,
not a Keycloak server administrator. Never import these development users into
QA/production; use an environment-specific realm provisioning process instead.

`mentor-web` is a public OpenID Connect client: Authorization Code and required
PKCE S256, no client secret, password grant, implicit flow or service account.
Local redirect URLs and origins are limited to localhost/127.0.0.1 port 5173.
An audience mapper adds `mentor-api` to access tokens, not ID tokens. Hosted
environments require their own exact HTTPS redirect/origin configuration.

Run `docker compose up -d postgres keycloak` with the required `.env` values.
The console is `http://localhost:8081`. Import skips a realm that already exists;
it does not update it or reset users. Review existing realm changes through the
admin process, without deleting data or overwriting the realm automatically.

With Docker and Python 3 available, run `python scripts/verify_keycloak.py` from
`backend/`. This creates a disposable Keycloak container with random passwords,
verifies three real Authorization Code/S256 logins, realm roles and API audience,
and rejects password-grant and missing-PKCE requests. Secrets and tokens stay in
memory or temporary files that are removed. CI runs this check too.

Reference: [Keycloak realm import](https://www.keycloak.org/server/importExport)
and [Keycloak client security](https://www.keycloak.org/docs/26.0.8/server_admin/index.html).

## JWT resource server (MEN-21)

Business routes require `Authorization: Bearer <access_token>`. The API verifies
the RS256 signature using Keycloak JWKS, exact issuer, audience `mentor-api`,
expiry/not-before and a nonempty subject. Missing expiry is rejected. ID tokens
for mentor-web are not API credentials. Wrong, expired or malformed tokens return
401 with a Bearer challenge; insufficient role permissions return 403.

Configure `KEYCLOAK_ISSUER_URI` and `KEYCLOAK_API_AUDIENCE` in local `.env`.
`KEYCLOAK_JWK_SET_URI` optionally selects the reachable key endpoint. Compose
uses Keycloak's internal hostname for JWKS and the frontend-visible URL as issuer;
these serve different purposes. Hosted issuer and JWKS URLs must use HTTPS.
Public-key lookup is lazy and cached: a reachable key endpoint is needed when
validating a token, not just to start public routes.

Health, hello, Swagger UI and OpenAPI remain public without a token. Other
management paths are denied, even to an application ADMIN. Only health is exposed
by Actuator configuration. The API is stateless with no login form, Basic auth,
cookie session or CSRF cookie. CSRF is disabled for bearer-only requests; MVC CORS
continues to enforce exact approved frontend origins and preflight headers.

Only realm roles STUDENT/TUTOR/ADMIN map to ROLE_ authorities; malformed, unknown
or client-role claims do not grant application roles. Method authorization is
enabled for future business policies. This does not implement user persistence.

Tests use real signed RSA JWTs and a temporary HTTP JWKS server, including bad
signature, issuer, audience, expiry, not-before, missing subject/expiry and role
denial. Existing public endpoint and CORS tests stay active.
Reference: [Spring Security JWT validation](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html).

## Authenticated identity (MEN-22)

`GET /api/v1/auth/me` requires a valid Keycloak API access token. Without it,
the response is 401. It returns exactly:

```json
{"id":"<Keycloak sub>","username":"tutor.local","email":"tutor@example.invalid","roles":["STUDENT","TUTOR"]}
```

`id` is the Keycloak subject, not an application database identifier. Username
and email reflect the corresponding token claims and can be null if omitted.
Roles are the verified, known realm roles already mapped by authentication,
sorted and deduplicated. No client-role grants, raw JWT, private claims, refresh
token or password are returned. Security response headers disable caching.
This endpoint does not create/update a user record or decide tutor eligibility.

Swagger describes the bearerAuth requirement for this route; public hello does
not require it. In frontend, obtain the access token using mentor-web PKCE, send
the Authorization header and handle 401 by refreshing/re-authenticating through
Keycloak. Do not send an ID token or calculate roles from unverified browser data
as authorization. The Keycloak verifier's `--expect-me` option checks all three
seeded identities against the actual packaged API, and CI runs the same check.

## Branches and workflow

| Branch | Purpose |
|---|---|
| `main` | Production. Changes only through a Pull Request approved by the reviewer. |
| `develop` | Integration and QA (default branch). Changes only through an approved Pull Request. |
| `feature/MEN-x-…`, `fix/…`, `chore/…`, `docs/…` | One branch per Jira task, created from `develop`. |

1. Create your branch from `develop` (Jira can create it from the task).
2. Commit with the Jira key, for example `feat: MEN-12 add booking endpoint`.
3. Open a Pull Request to `develop`. It needs 1 approval and the **CI passed** check in green.
4. Releases go from `develop` to `main` at the end of the sprint.

## CI/CD

GitHub Actions (`.github/workflows/backend-ci-cd.yml` at the root of the monorepo, only for changes in `backend`):

- **CI** on every Pull Request to `develop` or `main`: `mvn verify` (build + JUnit). It is skipped until `pom.xml` exists.
- **CD** after a merge: `develop` deploys to the **qa** environment and `main` to **production** (manual approval).

## Security

- Never commit passwords, tokens or `.env` files.
- QA and production secrets are stored in GitHub (Settings → Environments) or in the hosting provider.
- Payments are tested only in sandbox mode.
