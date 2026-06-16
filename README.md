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

The intent is to selfhost.
Use `docker compose` after editing the environment files. 

1. Copy and edit environment variables, e.g. `cp backend/.env.example backend/.env` and `cp frontend/.env.example frontend/.env`, then edit them with your values.
2. Build and start the services with `docker compose up --build`; if things look good, press `d` to detach.
3. Open the app in your browser at `http://localhost:3000` (or the port you set in `frontend/.env`).

You should now be good to go.

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

> State-changing requests (POST, PATCH, DELETE) require the `X-XSRF-TOKEN` header. 

An OpenAPI (Swagger) specification is available when the application is running, at `http://localhost:8080/v3/api-docs`, assuming the default port 8080 is used for the backend.

## Development & Fun Details

To work on Omiyage, use `docker compose` to start the backend, frontend, and database.
This requires you to have a valid `.env` file (see below).

```bash
# copy the env files
cp backend/.env.example backend/.env
cp frontend/.env.example frontend/.env
# edit them so that they make sense for you

# then build and run the project
docker compose up --build
```

### Authentication

Inspiration has been found in Mullvad's account number system.
Similar to them, Omiyage uses **passwordless authentication** as follows:

1. Users sign up and receive a randomly generated 16-digit `AccountNumber`.
2. This `AccountNumber` serves as both the username and password. It should be kept private.
3. No email or other personal information is required/collected. Nothing to leak.
4. Authentication state is managed via Spring Security, using an `XSRF-TOKEN` cookie/header for CSRF protection.

### Data Model

Omiyage's backend expects PostgreSQL version 18.

Since it's a wishlist application, we define the following entities:
- `User`: the owner of one or more lists.
- `List`: a wishlist, that can contain zero or more wishes.
- `Wish`: an item on a wishlist.

All relationships and schema definitions are managed through Flyway migrations.

### Object Storage

Besides PostgreSQL, the backend uses MinIO for object storage.

MinIO, which has S3 compatible API, is used for storing images.
This choice is made because storing images directly in postgres is _annoying_ and commonly done by storing a link. As such, images are stored as links to the MinIO service.

### API Integration

It is cumbersome to replicate data types between the backend and frontend manually.
As such, we use `openapi-typescript-codegen` to generate TypeScript types from the backend's OpenAPI specification.

To regenerate the types, run:

```bash
bun run generate:api
```

and you'll see them reflected in `src/lib/types-dto.ts`.

## Tests

If during testing you encounter a response conflict from docker daemon like shown below, this is because previously you have ran `docker compose up --build` to run the project. The end to end tests spin up using the same compose file in the backend dir, and thus try to create a container with the same name.

```
Error response from daemon: Conflict. The container name "/omiyage-minio" is already in use by container "909b7c9a90ef496fae26e27809051bc39891129f6bf58f69f6513bd5174b7ada". You have to remove (or rename) that container to be able to reuse that name.
```

To fix this, remove the containers, e.g.

```bash
docker container rm 909b7c9a90ef496fae26e27809051bc39891129f6bf58f69f6513bd5174b7ada
```


## Security Notes

- The account number is the login credential — treat it like a password. Share it with no one.
- In production, set `CORS_ALLOWED_ORIGINS` to the exact frontend origin instead of `*`.
- Use HTTPS in production (e.g. reverse proxy with TLS termination).
- Set strong `POSTGRES_PASSWORD`, `MINIO_ROOT_PASSWORD`, `MINIO_ACCESS_KEY`, and `MINIO_SECRET_KEY`.

