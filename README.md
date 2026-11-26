# Mentor

Peer tutoring platform for Universidad Central del Ecuador. Final project of the Software Architecture and Programming in English courses.

Monorepo: one repository for the whole project.

| Folder | Content |
| --- | --- |
| `apps/backend` | Spring Boot API (Java 21, modular monolith with DDD) and local Docker Compose |
| `apps/web-shell` | Web application (React + Vite + TypeScript) |
| `apps/landing` | Landing page (Astro) |

New apps and shared code (gateway, microfrontends, mobile, desktop, packages, infra, tests) are added under `apps/`, `packages/`, `infra/` and `tests/` when their Jira tasks start.

Documentation (architecture, diagrams, workflow, decisions): [Confluence – MENTOR](https://kevinteno11.atlassian.net/wiki/spaces/MEN/pages/65927)

Git flow: `feature/MEN-XX-description` -> Pull Request to `develop` (QA) -> Pull Request to `main` (production). Pull Request title: `[MEN-12] feat: add DDD module structure`.
