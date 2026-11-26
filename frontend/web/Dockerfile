# Mentor web app - React + Vite
# Multi-stage build: compile with Node, serve the static files with Nginx.

# ---------- Build ----------
FROM node:20-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
# Vite reads VITE_* variables at build time
ARG VITE_API_URL
ARG VITE_GRAPHQL_URL
ARG VITE_WS_URL
ARG VITE_KEYCLOAK_URL
ARG VITE_KEYCLOAK_REALM
ARG VITE_KEYCLOAK_CLIENT_ID
ARG VITE_STUN_URL
ENV VITE_API_URL=$VITE_API_URL \
    VITE_GRAPHQL_URL=$VITE_GRAPHQL_URL \
    VITE_WS_URL=$VITE_WS_URL \
    VITE_KEYCLOAK_URL=$VITE_KEYCLOAK_URL \
    VITE_KEYCLOAK_REALM=$VITE_KEYCLOAK_REALM \
    VITE_KEYCLOAK_CLIENT_ID=$VITE_KEYCLOAK_CLIENT_ID \
    VITE_STUN_URL=$VITE_STUN_URL
RUN npm run build

# ---------- Serve ----------
FROM nginx:1.27-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
