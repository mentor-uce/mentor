# Mentor – Backend

Mentor API: Java 21 + Spring Boot. Part of **Mentor – Learn and teach among peers** (Universidad Central del Ecuador).

- Web app and landing page: [mentor-frontend](https://github.com/mentor-uce/mentor-frontend)
- **Documentation (architecture, diagrams, workflow): [Confluence – MENTOR](https://kevinteno11.atlassian.net/wiki/spaces/MEN/pages/65927)**

## Run locally

The Spring Boot project (Maven, Java 21) goes in the root of this repository, next to this README.

1. Copy the environment variables and fill them in (never commit `.env`):
   ```bash
   cp .env.example .env
   ```
2. Start PostgreSQL, MongoDB, Redis and Keycloak:
   ```bash
   docker compose up -d
   ```
3. Run the API (http://localhost:8080):
   ```bash
   ./mvnw spring-boot:run
   ./mvnw verify    # build and tests
   ```

To run the whole application with Docker, clone [mentor-frontend](https://github.com/mentor-uce/mentor-frontend) next to this repository and run `docker compose --profile app up --build`. Add `--profile automation` to start n8n.
