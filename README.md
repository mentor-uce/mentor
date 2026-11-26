# Mentor

Peer tutoring platform for Universidad Central del Ecuador. Final project of the Software Architecture and Programming in English courses.

Monorepo layout and decisions: `docs/Mentor_Documento_general_del_sistema_v2.1.md` (section 3). It replaces the former `mentor-backend` and `mentor-frontend` repositories; their history is preserved. Existing code lives in `apps/backend`, `apps/web-shell` and `apps/landing`; the other folders are placeholders to be filled by their Jira tasks.

| Folder | Content |
| --- | --- |
| apps/ | backend, gateway, web-shell, mfe-tutoring, mfe-admin, landing, mobile, desktop |
| packages/ | design-system, api-client, contracts, wasm-health |
| infra/ | docker, aws |
| workflows/n8n/ | exported n8n workflows |
| tests/ | E2E, load and integration tests |
| docs/ | specification, ADRs, diagrams, runbooks, evidence |

Git flow: `feature/MEN-XX-description` -> PR to `develop` -> CI -> QA -> PR to `main`. PR title: `[MEN-12] feat: add DDD module structure`.
