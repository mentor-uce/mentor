-- Runs only the first time the PostgreSQL container starts (empty volume).
-- Main database (POSTGRES_DB) is created by the image itself.

-- Vector search for embeddings (AI semantic search)
CREATE EXTENSION IF NOT EXISTS vector;

-- Separate databases for Keycloak and n8n, in the same PostgreSQL server
CREATE DATABASE keycloak;
CREATE DATABASE n8n;
