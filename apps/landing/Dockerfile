# Mentor landing page - Astro
# Multi-stage build: compile the static site, serve it with Nginx.

# ---------- Build ----------
FROM node:20-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
ARG PUBLIC_APP_URL
ENV PUBLIC_APP_URL=$PUBLIC_APP_URL
RUN npm run build

# ---------- Serve ----------
FROM nginx:1.27-alpine
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
