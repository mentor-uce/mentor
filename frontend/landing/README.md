# Mentor - Landing Page

## Project Description

The Mentor Landing Page is the public website for the peer-to-peer academic tutoring platform of the Central University of Ecuador (UCE).

Its purpose is to present Mentor's value proposition, communicate its core features, and make it easier for students and tutors to access the platform through sign-up and sign-in options.

This application serves as the public presentation layer of the Mentor ecosystem and is built with Astro using Static Site Generation (SSG), prioritizing performance, accessibility, SEO, and a responsive experience.

## 2. Technologies Used

| Technology | Purpose |
| :--- | :--- |
| **Astro** | Main framework for building the Landing Page. |
| **Tailwind CSS** | Utility-first system for building interfaces and styles. |
| **TypeScript** | Static typing support when required. |
| **HTML5** | Semantic content structure. |
| **CSS3** | Style customization and responsive design. |
| **Git and GitHub** | Version control and collaboration. |
| **Figma** | Interface design and prototyping. |

## 3. Architecture and Development Approach

The Landing Page uses Astro with Static Site Generation (SSG).

This approach generates optimized static files for deployment, reducing the need for runtime processing and improving site performance.

The application remains separate from Mentor's main SPA, which is built with React, Vite, and TypeScript.

### Development Principles

* **SOLID:** Maintain clear responsibilities and a maintainable organization.
* **DRY:** Avoid unnecessary code duplication.
* **KISS:** Prioritize simple, easy-to-understand solutions.
* **YAGNI:** Implement only the features required for the defined scope.

## 4. Landing Page Features and Content

The Landing Page includes the following sections:

* **Hero:** Introduces Mentor and its value proposition, featuring the tagline *"Teaching is also learning"* (*"Enseñar también es aprender"*).
* **Problem:** Context on peer academic support needs among students.
* **How it works:** Explanation of how Mentor works from both the student and tutor perspectives.
* **Benefits:** Advantages of using the platform.
* **Trust:** Information about the institutional context and how the platform operates.
* **FAQ:** Answers to the main questions about Mentor.
* **Footer:** Institutional information, links, and legal documentation.

### Primary Actions

* **Sign up** (*Registrarme*).
* **Sign in** (*Iniciar sesión*).

These actions will take users to the corresponding flows on the Mentor platform.

## 5. Design and Visual Identity

The Landing Page design must follow the Design System defined for Mentor and the visual prototype created in Figma.

### Color Palette

| Color | Code |
| :--- | :--- |
| Night (*Noche*) | `#0B1220` |
| Sky (*Cielo*) | `#38BDF8` |
| Indigo (*Índigo*) | `#6366F1` |
| Turquoise (*Turquesa*) | `#2DD4BF` |
| Deep Turquoise (*Turquesa profundo*) | `#0D9488` |
| Ice (*Hielo*) | `#E6F4F8` |

### Typography

* **Poppins:** Titles and headings.
* **Inter:** Body text and general content.

### Responsive Design

The following reference resolutions are considered:

| Device | Resolution |
| :--- | :--- |
| Mobile | `390 px` |
| Mobile | `414 px` |
| Tablet | `768 px` |
| Desktop | `1280 px` |

The interface must adapt to different screen sizes and comply with **WCAG 2.2** accessibility guidelines.

## 6. Project Structure

The initial application layout follows this structure:

```text
landing/
├── public/
│   ├── images/
│   ├── icons/
│   └── fonts/
│
├── src/
│   ├── components/
│   ├── layouts/
│   ├── pages/
│   └── styles/
│
├── astro.config.mjs
├── package.json
├── tsconfig.json
└── README.md
```

### Directory Descriptions

| Directory | Responsibility |
| :--- | :--- |
| `public/` | Static assets, images, icons, and fonts. |
| `components/` | Reusable UI components for the Landing Page. |
| `layouts/` | Page templates and general layout structures. |
| `pages/` | Application pages and routes. |
| `styles/` | Global styles and visual configuration. |

This structure may evolve as implementation progresses, while respecting the project's architectural guidelines.

## 7. Prerequisites

Running the project requires:

* **Node.js** in a version compatible with Astro.
* **pnpm**, according to the repository configuration.
* **Git**.
* **Visual Studio Code** (recommended).

## 8. Installation and Setup

### 8.1. Clone the Repository

Clone Mentor's frontend repository from GitHub, following the established folder structure and workflow conventions.

### 8.2. Install Dependencies

From the monorepo root, run:

```bash
pnpm install
```

### 8.3. Navigate to the Application

```bash
cd apps/landing
```

### 8.4. Run in Development Environment

```bash
pnpm dev
```

The application will be available at the local URL provided by Astro, typically:

```text
http://localhost:4321
```

## 9. Available Commands

| Command | Description |
| :--- | :--- |
| `pnpm dev` | Starts the local development server. |
| `pnpm build` | Generates the production build. |
| `pnpm preview` | Lets you preview the generated build. |
| `pnpm astro check` | Runs Astro checks when the required dependencies are configured. |

Commands should be run from the application directory whenever the scripts are defined in its `package.json`.

## 10. Quality and Technical Criteria

The Landing Page implementation must account for:

* Static generation with Astro.
* Responsive design.
* Semantic HTML.
* Accessibility compliant with WCAG 2.2.
* Asset and image optimization.
* SEO best practices.
* Reusable components.
* Code organization and maintainability.
* Validation via Lighthouse.
* Error-free build.

## 11. Deployment

The documented architecture plans to publish the Landing Page using **AWS S3** and **CloudFront**.

Infrastructure, domain, certificate, and deployment environment setup will take place in the corresponding phase, in accordance with Mentor's deployment and operations documentation.

## 12. Version Control

**GitHub** will be used for version control.

Feature branches must follow the convention:

```text
feature/MEN-<n>-descripcion
```

Commits must follow **Conventional Commits** and include a reference to the corresponding Jira ticket.

Example:

```text
feat(MEN-7): inicializar landing page con Astro
```

Changes will be integrated via **Pull Requests**, following the repository's review and approval rules.

## 13. Reference Documentation

Implementation must stay aligned with the **Mentor v2.1** technical documentation:

* Document 00: Index and scope.
* Document 01: Folder structure and architecture.
* Document 03: Contexts, subdomains, and invariants.
* Document 05: API contracts.
* Document 07: Deployment and operations.
* Document 08: Flow and route map.
* Document 09: Frontend requirements matrix.

Centralized technical documentation and its architectural decisions serve as the reference source for development.

---

* **Project:** Mentor
* **Application:** Landing Page
* **Framework:** Astro
* **Styling:** Tailwind CSS
* **Rendering:** Static Site Generation (SSG)
* **Status:** In development