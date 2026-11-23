# Mentor – Frontend

Mentor web application (React + Vite + TypeScript), responsive for mobile and desktop, and the public landing page (Astro).

Part of **Mentor – Learn and teach among peers**, a peer-tutoring web platform for students of Universidad Central del Ecuador.

| Repository | Content |
|---|---|
| [mentor-frontend](https://github.com/mentor-uce/mentor-frontend) | React web app and Astro landing page (this repository) |
| [mentor-backend](https://github.com/mentor-uce/mentor-backend) | Spring Boot API |
| [mentor](https://github.com/mentor-uce/mentor) | Docker Compose, architecture diagrams and shared documentation |

**Owner:** Narváez Milisen (@Milisen03Narvaez)

## Structure

| Folder | Content |
|---|---|
| `/` (root) | Web application: React + Vite + TypeScript |
| `landing/` | Landing page: Astro + Tailwind |

## Tech stack

- React with Vite and TypeScript
- Tailwind CSS
- Zustand (global state)
- TanStack Query and Axios (API calls)
- React Hook Form (forms; Formik as alternative)
- Login with Keycloak (OIDC + PKCE)
- Vitest (unit tests) and Playwright (end-to-end tests)
- Astro + Tailwind for the landing page

## Getting started

### Web application

1. Create the base project with Vite (React + TypeScript) **in the root of this repository**, so that `package.json` is next to this README:
   ```bash
   npm create vite@latest . -- --template react-ts
   ```
2. Copy `.env.example` to `.env` and fill in the values (never commit `.env`).

```bash
npm install
npm run dev     # http://localhost:5173
npm run build
npm test        # Vitest
```

### Landing page

Create the Astro project **inside `landing/`**, so that `landing/package.json` exists:

```bash
cd landing
npm install
npm run dev     # http://localhost:4321
npm run build
```

Each part has its own `Dockerfile`: it builds the static files and serves them with Nginx.

## Branches and workflow

| Branch | Purpose |
|---|---|
| `main` | Production. Changes only through a Pull Request approved by the reviewer. |
| `develop` | Integration and QA (default branch). Changes only through an approved Pull Request. |
| `feature/MEN-x-…`, `fix/…`, `chore/…`, `docs/…` | One branch per Jira task, created from `develop`. |

1. Create your branch from `develop` (Jira can create it from the task).
2. Commit with the Jira key, for example `feat: MEN-4 add login page`.
3. Open a Pull Request to `develop`. It needs 1 approval and the **CI passed** check in green.
4. Releases go from `develop` to `main` at the end of the sprint.

## CI/CD

GitHub Actions (`.github/workflows/ci-cd.yml`):

- **CI** on every Pull Request to `develop` or `main`: install, lint, Vitest and build for the web app, and build for the landing page. Each part is skipped until its `package.json` exists.
- **CD** after a merge: `develop` deploys to the **qa** environment and `main` to **production** (manual approval).

## Security

- Never commit passwords, tokens or `.env` files.
- Only public values go in `VITE_*` variables: they end up in the browser.
- QA and production values are stored in GitHub (Settings → Environments) or in the hosting provider.
