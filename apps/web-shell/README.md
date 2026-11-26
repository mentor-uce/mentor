# Mentor – Frontend

Mentor web app (React + Vite + TypeScript) and landing page (Astro, in `apps/landing/`). Part of **Mentor – Learn and teach among peers** (Universidad Central del Ecuador).

- API and local environment (Docker Compose): `apps/backend` of this monorepo
- **Documentation (architecture, diagrams, workflow): [Confluence – MENTOR](https://kevinteno11.atlassian.net/wiki/spaces/MEN/pages/65927)**

## Run locally

The Vite project goes in the root of this repository (`npm create vite@latest . -- --template react-ts`) and the Astro project in `landing/`.

1. Copy the environment variables (never commit `.env`):
   ```bash
   cp .env.example .env
   ```
2. Web app (http://localhost:5173):
   ```bash
   npm install
   npm run dev
   npm test        # Vitest
   ```
3. Landing page (http://localhost:4321):
   ```bash
   cd landing
   npm install
   npm run dev
   ```

The API, databases and Keycloak start from `apps/backend` with `docker compose up -d`.
