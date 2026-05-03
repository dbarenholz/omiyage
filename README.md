# Omiyage

A selfhosted wishlist application, created solely by vibe coding as an experiment, and to learn how to use these technologies.
Choice of technology is my own, namely Spring Boot for the backend and SvelteKit for the frontend.
This because I'm relatively familiar with them, and the hope was that I could fix hallucinations faster than with tech I'm less familiar with.
Everything below this line is originally LLM generated -- use with caution.

## Features

- **Passwordless auth** — sign up to receive a 16-digit account number; that's your login. Nothing to leak.
- **Multiple wishlists per user** — organise by occasion, season, etc.
- **Wishes** with optional description, approximate price, purchase links, optional image URL/upload, and arbitrary text tags (`#tech`, `#books`, …).
- **Wish claiming** — logged-in users can claim/unclaim a wish to avoid duplicate gifts. The list owner never sees claim status (surprise preserved!).
- **Public sharing** — every list has a secret share URL; anyone with the link can browse it without an account.
- **Dark mode** (Halcyon colour scheme), mobile-first, minimal UI.

## Work in Progress

- **Public sharing** — every list has a secret share URL (like Google Drive). Anyone with the link can browse it without an account.

## Tech Stack

| Layer          | Technology                               |
| -------------- | ---------------------------------------- |
| Backend        | Java 25, Spring Boot 3, JPA              |
| Database       | PostgreSQL 16                            |
| Migrations     | Flyway                                   |
| Object Storage | MinIO (S3-compatible)                    |
| Frontend       | SvelteKit 2 (Svelte 5), TypeScript, Bun  |
| Packaging      | Docker + Docker Compose                  |

## Quick Start

The intent is to selfhost. I suggest you use `docker compose` after editing the environment files.

1. Copy and edit service environment variables, e.g. `cp backend/.env.example backend/.env` and `cp frontend/.env.example frontend/.env`, then edit them with your values.
2. Build and start the services with `docker compose up --build -d`; note that `-d` is for detached mode, so logs won't show in the terminal.
3. Open the app in your browser at `http://localhost:3000` (or the port you set in `frontend/.env`).

### Default ports

| Service       | Host port   |
| ------------- | ----------- |
| Frontend      | 3000        |
| Backend       | 8080        |
| Database      | 5432        |
| MinIO API     | 9000        |
| MinIO Console | 9001        |

### MinIO

The stack includes MinIO for wish image uploads.

- API endpoint: `http://localhost:9000`
- Console: `http://localhost:9001`
- Default local credentials (from `backend/.env`): `minioadmin` / `minioadmin`

## Reverse Proxy Deployment

> [CAUTION]
> Not yet tested for accuracy! Use with caution and verify all settings.

### Subdomain (`omiyage.example.com`)

Point your reverse proxy at the frontend container on port `3000` and a separate subdomain/path for the API on port `8080`. Set:

```env
PUBLIC_API_URL=https://api.omiyage.example.com
PUBLIC_API_PORT=
PUBLIC_BASE_PATH=
CORS_ALLOWED_ORIGINS=https://omiyage.example.com
```

### Subpath (`example.com/omiyage`)

```env
PUBLIC_API_URL=https://example.com
PUBLIC_API_PORT=
PUBLIC_API_PATH=/omiyage/api
PUBLIC_BASE_PATH=/omiyage
SERVER_SERVLET_CONTEXT_PATH=/omiyage/api
CORS_ALLOWED_ORIGINS=https://example.com
```

Then configure your reverse proxy (nginx/Caddy/Traefik) to forward:

- `example.com/omiyage/*` → frontend container
- `example.com/omiyage/api/*` → backend container

#### Nginx example

```nginx
location /omiyage/ {
    proxy_pass http://frontend:3000/;
    proxy_set_header Host $host;
}

location /omiyage/api/ {
    proxy_pass http://backend:8080/omiyage/api/;
    proxy_set_header Host $host;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
}
```

## Environment Variables

### Backend (`backend/.env`)

| Variable                           | Default                 | Description                                                       |
|------------------------------------|-------------------------|-------------------------------------------------------------------|
| `POSTGRES_DB`                      | `omiyage`               | Database name                                                     |
| `POSTGRES_USER`                    | `omiyage`               | Database user                                                     |
| `POSTGRES_PASSWORD`                | *(required)*            | Database password                                                 |
| `SERVER_PORT`                      | `8080`                  | Backend container port                                            |
| `SERVER_SERVLET_CONTEXT_PATH`      | `/`                     | Backend servlet context path                                      |
| `CORS_ALLOWED_ORIGINS`             | `*`                     | Comma-separated allowed origins; use exact origins in production. |
| `CLEANUP_INACTIVE_ACCOUNT_MINUTES` | `15`                    | Minutes before a never-logged-in account is deleted               |
| `MINIO_ROOT_USER`                  | `minioadmin`            | MinIO root username (used by MinIO container)                     |
| `MINIO_ROOT_PASSWORD`              | `minioadmin`            | MinIO root password (used by MinIO container)                     |
| `MINIO_ENDPOINT`                   | `http://minio:9000`     | Internal MinIO endpoint used by backend                           |
| `MINIO_PUBLIC_URL`                 | `http://localhost:9000` | Public base URL used for generated image URLs                     |
| `MINIO_ACCESS_KEY`                 | `minioadmin`            | MinIO access key                                                  |
| `MINIO_SECRET_KEY`                 | `minioadmin`            | MinIO secret key                                                  |
| `MINIO_BUCKET`                     | `omiyage-images`        | Bucket name for wish images                                       |

The backend can also read optional `DATABASE_URL`, `DATABASE_USERNAME`, and `DATABASE_PASSWORD` overrides. If they are unset, Spring derives them from `POSTGRES_DB`, `POSTGRES_USER`, and `POSTGRES_PASSWORD`.

### Frontend (`frontend/.env`)

| Variable           | Default            | Description                                |
| ------------------ | ------------------ | ------------------------------------------ |
| `PUBLIC_API_URL`   | `http://localhost` | URL the browser uses to reach the API      |
| `PUBLIC_API_PORT`  | `8080`             | API port as seen by the browser            |
| `PUBLIC_API_PATH`  | `/api`             | API base path                              |
| `PUBLIC_BASE_PATH` | *(empty)*          | Frontend base path for subpath deployments |
| `PORT`             | `3000`             | Frontend container port                    |

The Compose file now reads `backend/.env` for the database, backend, and MinIO services, and `frontend/.env` for the frontend service. It no longer uses a root `.env` file.

## Development (without Docker)

> [CAUTION]
> Not yet tested for accuracy! Use with caution and verify all settings. You may look at manually written `run.sh` in the backend folder for reference.

### Backend

```bash
cd backend
# Requires Java 25, Maven 3.9+, and a running PostgreSQL instance
export DATABASE_URL=jdbc:postgresql://localhost:5432/omiyage
export DATABASE_USERNAME=omiyage
export DATABASE_PASSWORD=omiyage
export SERVER_PORT=8080
mvn spring-boot:run
```

### Frontend

```bash
cd frontend
bun install
# Point at the running backend
PUBLIC_API_URL=http://localhost PUBLIC_API_PORT=8080 PUBLIC_API_PATH=/api bun run dev
```

The frontend now runs `svelte-kit sync` automatically during `bun install` and before the standard `dev`, `build`, and `preview` commands so the generated `.svelte-kit/tsconfig.json` exists before TypeScript tooling reads it.

## API Overview

All endpoints are prefixed with `/api`.

| Method | Path                          | Auth      | Description                      |
| ------ | ----------------------------- | --------- | -------------------------------- |
| POST   | `/api/auth/signup`            | —         | Create account, returns `accountNumber` |
| POST   | `/api/auth/login`             | —         | Login with `{accountNumber}`     |
| POST   | `/api/auth/logout`            | ✓         | End session                      |
| GET    | `/api/auth/me`                | ✓         | Current user info                |
| GET    | `/api/auth/csrf`              | —         | Issue/refresh CSRF token         |
| PATCH  | `/api/users/me`               | ✓         | Update display name              |
| GET    | `/api/lists`                  | ✓         | My lists                         |
| POST   | `/api/lists`                  | ✓         | Create list                      |
| GET    | `/api/lists/{listId}`         | ✓         | Get list details                 |
| PATCH  | `/api/lists/{listId}`         | ✓ (owner) | Update list                      |
| DELETE | `/api/lists/{listId}`         | ✓ (owner) | Delete list                      |
| GET    | `/api/lists/shared/{shareId}` | —         | Public list view                 |
| GET    | `/api/lists/{listId}/wishes`  | ✓ (owner) | Get wishes in a list             |
| POST   | `/api/lists/{listId}/wishes`  | ✓ (owner) | Add wish                         |
| PATCH  | `/api/lists/{listId}/wishes/{wishId}` | ✓ (owner) | Update wish               |
| DELETE | `/api/lists/{listId}/wishes/{wishId}` | ✓ (owner) | Delete wish               |
| POST   | `/api/uploads/images`         | ✓         | Upload wish image (multipart)    |
| POST   | `/api/wishes/{wishId}/claim`  | ✓         | Claim wish                       |
| DELETE | `/api/wishes/{wishId}/claim`  | ✓         | Unclaim wish                     |

> State-changing requests require the `X-XSRF-TOKEN` header (value from the `XSRF-TOKEN` cookie).

## Security Notes

- The account number is the login credential — treat it like a password. Share it with no one.
- In production, set `CORS_ALLOWED_ORIGINS` to the exact frontend origin instead of `*`.
- Use HTTPS in production (reverse proxy with TLS termination).
- Set a strong `POSTGRES_PASSWORD`.
