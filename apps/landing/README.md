# Landing page

Mentor public landing page, separate from the React web application.

## Tech stack

- Astro
- Tailwind CSS

## Commands

Create the Astro project **directly inside this folder**, so that `landing/package.json` exists:

```bash
npm create astro@latest .
npm install
npm run dev     # http://localhost:4321
npm run build
```

`PUBLIC_APP_URL` (see `.env.example` in the repository root) is the address of the web application used by the sign-up and login links.

The `Dockerfile` in this folder builds the static site and serves it with Nginx.
