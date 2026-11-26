# Mentor

Peer tutoring platform for Universidad Central del Ecuador. Final project of the Software Architecture and Programming in English courses.

Monorepo: one repository, with the backend and the frontend in separate folders.

| Folder | Content |
| --- | --- |
| `backend/` | Spring Boot API (Java 21, modular monolith with DDD) and local Docker Compose |
| `frontend/web/` | Web application (React + Vite + TypeScript) |
| `frontend/landing/` | Landing page (Astro) |

Each side has its own CI/CD workflow in `.github/workflows/` that runs only when its folder changes.

Documentation (architecture, diagrams, workflow, decisions): [Confluence – MENTOR](https://kevinteno11.atlassian.net/wiki/spaces/MEN/pages/65927)

Git flow: `feature/MEN-XX-description` -> Pull Request to `develop` (QA) -> Pull Request to `main` (production). Pull Request title: `[MEN-12] feat: add DDD module structure`.
