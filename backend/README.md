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
- HTTP smoke and startup connectivity tests against temporary PostgreSQL containers

The application now requires a reachable PostgreSQL database and its credentials. Keycloak and payment integrations remain planned.
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
Start PostgreSQL before running the API; see MEN-16 below.
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
Health now includes PostgreSQL connectivity in its aggregate status, while component details stay hidden. Other provider connectivity remains outside this task.

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
docker compose up -d postgres
docker compose ps postgres
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
and Open Session in View is disabled. No entities, business tables or migrations
are added. Flyway and `V1__init.sql` belong to MEN-17. With incorrect credentials
or an unreachable database, startup fails; do not treat a disconnected API as healthy.

`mvn verify` requires Docker: Testcontainers starts PostgreSQL 16 with ephemeral
credentials and random ports, runs `SELECT 1`, checks JPA initialization and
runs the existing HTTP checks against the connected application. It does not
use your local `.env`, database or credentials; containers are stopped afterward.
CI runners already provide Docker. Reference:
[Spring Boot SQL configuration](https://docs.spring.io/spring-boot/reference/data/sql.html)
and [Testcontainers service connections](https://docs.spring.io/spring-boot/reference/testing/testcontainers.html).

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
