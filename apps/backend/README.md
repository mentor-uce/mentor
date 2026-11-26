# Mentor – Backend

Mentor API: Spring Boot (Java 21) modular monolith with DDD.

Part of **Mentor – Learn and teach among peers**, a peer-tutoring web platform for students of Universidad Central del Ecuador.

| Repository | Content |
|---|---|
| [mentor-backend](https://github.com/mentor-uce/mentor-backend) | Spring Boot API (this repository) |
| [mentor-frontend](https://github.com/mentor-uce/mentor-frontend) | React web app and Astro landing page |
| [mentor](https://github.com/mentor-uce/mentor) | Docker Compose, architecture diagrams and shared documentation |

**Owner:** Alquinga Cristopher (@Cristopher2912)

- Web app and landing page: `apps/web-shell` and `apps/landing` of this monorepo
- **Documentation (architecture, diagrams, workflow): [Confluence – MENTOR](https://kevinteno11.atlassian.net/wiki/spaces/MEN/pages/65927)**

## Current implementation (MEN-11)

- Java 21 and Spring Boot 4.2.0
- Spring MVC with an embedded HTTP server
- Maven 3.10.1 through the checked-in Maven Wrapper
- An automated Spring application context startup test

The base application starts without databases, Keycloak or payment credentials.
Business modules, health checks, Swagger and external integrations belong to
their respective Jira tasks. Visiting `/` currently returns HTTP 404 because
no business routes exist yet.

## Target tech stack

- Java 21 + Spring Boot
- Spring Security as an OAuth 2.0 resource server (validates the Keycloak JWT and applies roles)
- REST /api/v1 documented with OpenAPI (springdoc) and GraphQL (Spring for GraphQL)
- WebSocket (STOMP) for chat, live status and WebRTC signaling
- Spring Data JPA (PostgreSQL + pgvector), Spring Data MongoDB and Spring Data Redis
- Spring Boot Actuator (`/actuator/health` is used by the Docker health check)
- JUnit Jupiter and Mockito for tests (versions managed by Spring Boot)
- Maven as the build tool

## Package structure (MEN-12)

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

Run the commands from `apps/backend` of the monorepo, where `pom.xml` lives.

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
.\mvnw.cmd spring-boot:run     # run locally on port 8080
```

### Linux / macOS

```bash
./mvnw -v
./mvnw clean verify
./mvnw spring-boot:run
```

### Run the packaged application

After a successful build:

```bash
java -jar target/mentor-backend-0.0.1-SNAPSHOT.jar
```

The default port is `8080`. Set `SERVER_PORT` or pass `--server.port=8082` to
change it. Stop the application with `Ctrl+C`.

The application needs no service credentials or application-specific environment
variables for this base local run. The `.env.example` file documents future
integrations; Spring Boot does not automatically load it during a direct run.
When those integrations are implemented, start their infrastructure using the
Docker Compose file in this folder (`docker-compose.yml`):
`docker compose up -d` starts PostgreSQL, MongoDB, Redis and Keycloak; add
`--profile automation` to start n8n. Never commit `.env` files or real credentials.

The existing `Dockerfile` builds the application with Maven and runs the JAR on
port 8080. The `/actuator/health` endpoint is not provided by MEN-11; health checks
are part of MEN-13.

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

GitHub Actions (`.github/workflows/backend-ci-cd.yml` at the root of the monorepo, only for changes in `apps/backend`, `apps/gateway` and `packages/contracts`):

- **CI** on every Pull Request to `develop` or `main`: `mvn verify` (build + JUnit). It is skipped until `pom.xml` exists.
- **CD** after a merge: `develop` deploys to the **qa** environment and `main` to **production** (manual approval).

## Security

- Never commit passwords, tokens or `.env` files.
- QA and production secrets are stored in GitHub (Settings → Environments) or in the hosting provider.
- Payments are tested only in sandbox mode.
