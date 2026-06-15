# Omiyage

A selfhosted wishlist application, created largely by vibe coding as an experiment.
Choice of technology is my own, namely Spring Boot for the backend and SvelteKit for the frontend.
This because I'm relatively familiar with them, so that I can fix hallucinations.


## Features

- **Passwordless authentication**: sign up to receive a 16-digit account number; that's the login and password. No personal info to leak.
- **Multiple wishlists per user**: organise by occasion, season, etc.
- **Wishes** with (optional) description, price, links, optional image, and arbitrary tags such as `#tech`, `#books`, ...
- **List sharing** every list has a secret share URL; anyone with the link can browse it without an account. This is intended to allow sharing lists publically, while the application runs on private infrastructure.
- **Wish claiming**: logged-in users can claim/unclaim a wish to avoid duplicate gifts. The list owner never sees claim status (surprise preserved!).

## Quickstart

<!-- TODO: this should explain how to run the project when a tagged docker container has been published for it -->

The intent is to selfhost. Use `docker compose` after editing the environment files. 

1. Copy and edit environment variables, e.g. `cp backend/.env.example backend/.env` and `cp frontend/.env.example frontend/.env`, then edit them with your values.
2. Build and start the services with `docker compose up --build`; if things look good, press `d` to detach.
3. Open the app in your browser at `http://localhost:3000` (or the port you set in `frontend/.env`).

You should now be good to go.

## Development

This project uses following tech:

| Layer          | Technology                               |
| -------------- | ---------------------------------------- |
| Backend        | Java 25, Spring Boot 4, JPA              |
| Database       | PostgreSQL 18                            |
| Migrations     | Flyway                                   |
| Object Storage | MinIO (S3-compatible)                    |
| Frontend       | SvelteKit 2 (Svelte 5), TypeScript, Bun  |
| Testing        | Vitest (unit) + Playwright (e2e)         | 
| Packaging      | Docker + Docker Compose                  |

To run the project, use `docker compose up --build` from the root dir after cloning.
This will spin up the database, backend, and frontend, and connect them together
using the values in the environment files. If you haven't set up the `.env` files in the frontend and backend directories, you probably want to do so.

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

The backend can also read optional `DATABASE_URL`, `DATABASE_USERNAME`, and `DATABASE_PASSWORD` overrides.
If they are unset, Spring derives them from `POSTGRES_DB`, `POSTGRES_USER`, and `POSTGRES_PASSWORD`.

### Frontend (`frontend/.env`)

| Variable           | Default            | Description                                |
| ------------------ | ------------------ | ------------------------------------------ |
| `PUBLIC_API_URL`   | `http://localhost` | URL the browser uses to reach the API      |
| `PUBLIC_API_PORT`  | `8080`             | API port as seen by the browser            |
| `PUBLIC_API_PATH`  | `/api`             | API base path                              |
| `PUBLIC_BASE_PATH` | *(empty)*          | Frontend base path for subpath deployments |
| `PORT`             | `3000`             | Frontend container port                    |

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
- Set strong `POSTGRES_PASSWORD`, `MINIO_ROOT_PASSWORD`, `MINIO_ACCESS_KEY`, and `MINIO_SECRET_KEY`.

