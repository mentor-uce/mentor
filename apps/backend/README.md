# Mentor – Backend

Mentor API: Spring Boot (Java 21) modular monolith with DDD.

Part of **Mentor – Learn and teach among peers**, a peer-tutoring web platform for students of Universidad Central del Ecuador.

| Repository | Content |
|---|---|
| [mentor-backend](https://github.com/mentor-uce/mentor-backend) | Spring Boot API (this repository) |
| [mentor-frontend](https://github.com/mentor-uce/mentor-frontend) | React web app and Astro landing page |
| [mentor](https://github.com/mentor-uce/mentor) | Docker Compose, architecture diagrams and shared documentation |

**Owner:** Alquinga Cristopher (@Cristopher2912)

## Tech stack

- Java 21 + Spring Boot
- Spring Security as an OAuth 2.0 resource server (validates the Keycloak JWT and applies roles)
- REST /api/v1 documented with OpenAPI (springdoc) and GraphQL (Spring for GraphQL)
- WebSocket (STOMP) for chat, live status and WebRTC signaling
- Spring Data JPA (PostgreSQL + pgvector), Spring Data MongoDB and Spring Data Redis
- Spring Boot Actuator (`/actuator/health` is used by the Docker health check)
- JUnit 5 and Mockito for tests
- Maven as the build tool

## Planned modules

Auth, Users, Services, Schedules, Bookings, Sessions, Payments, Messaging, Video, Reviews, Notifications and Admin.

## Getting started

1. Create the base project with [Spring Initializr](https://start.spring.io) (Maven, Java 21) **in the root of this repository**, so that `pom.xml` is next to this README.
2. Start the databases and Keycloak with the Docker Compose file of the [mentor](https://github.com/mentor-uce/mentor) repository.
3. Copy `.env.example` to `.env` and fill in the values (never commit `.env`).

```bash
./mvnw spring-boot:run   # run locally on http://localhost:8080
./mvnw verify            # build and run the tests
```

The `Dockerfile` builds the application with Maven and runs it on port 8080.

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

GitHub Actions (`.github/workflows/ci-cd.yml`):

- **CI** on every Pull Request to `develop` or `main`: `mvn verify` (build + JUnit). It is skipped until `pom.xml` exists.
- **CD** after a merge: `develop` deploys to the **qa** environment and `main` to **production** (manual approval).

## Security

- Never commit passwords, tokens or `.env` files.
- QA and production secrets are stored in GitHub (Settings → Environments) or in the hosting provider.
- Payments are tested only in sandbox mode.
