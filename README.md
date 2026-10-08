# Customer Account Statements

A microservices system for uploading, storing, and retrieving customer account statements. An API gateway fronts three
Spring Boot services and authenticates every request against [Dex](https://dexidp.io/) (OIDC). Statement files are
stored in S3 — emulated locally by [MiniStack](https://github.com/ministackorg/ministack) — while documents and users
each live in their own PostgreSQL database, and pre-signed download URLs are cached in Redis.

## Architecture

| Service             | Role                                                               | Host port |
|---------------------|--------------------------------------------------------------------|-----------|
| `api-gateway`       | Single entry point; validates the Dex token and routes traffic     | **8080**  |
| `login-service`     | Exchanges credentials for a Dex token                              | internal  |
| `statement-service` | Uploads/retrieves statements (S3 + `customer_statements` DB)       | internal  |
| `user-service`      | Owns users & roles (`users` DB)                                    | internal  |
| `dex`               | OIDC identity provider (seeded users)                             | 5556      |
| `ministack`         | Local AWS S3 emulator                                             | 4566      |
| `statements-db`     | PostgreSQL — document metadata                                    | internal  |
| `user-db`           | PostgreSQL — users & roles                                        | internal  |
| `redis`             | Pre-signed URL cache                                              | internal  |

Only `8080` (gateway), `5556` (Dex), and `4566` (MiniStack) are published to the host; everything else is reachable only
inside the Docker network.

**Request flow:** a client sends `Authorization: Bearer <idToken>` to the gateway. The gateway validates the token,
decodes the Dex subject into a user id, and forwards it downstream as the `X-User-Id` header. It then routes `/api/auth/**` → login-service, `/api/statements/**` → statement-service, and
`/api/users/**` → user-service.

## Prerequisites

- [Docker](https://docs.docker.com/get-docker/) and Docker Compose.

## Running locally

From the project root:

```bash
docker compose up --build
```

This builds and starts the full stack (gateway, the three services, Dex, MiniStack, two Postgres databases, and Redis).
Once healthy, the API is available at `http://localhost:8080`.

To stop everything:

```bash
docker compose down
```

All local configuration has working defaults (in `docker-compose.yaml` and each service's
`application-local.yaml`), so no `.env` file is required.

## Authentication & seeded users

Authentication is handled by **Dex** using in-memory users defined in [`config.yaml`](config.yaml). The **username is
the email**. Log in to receive an `idToken`, then send it as `Authorization: Bearer <idToken>` on every other request.

| Email               | Password   | Role  |
|---------------------|------------|-------|
| `admin@example.com` | `password` | ADMIN |
| `test1@example.com` | `test1`    | USER  |
| `test2@example.com` | `test2`    | USER  |
| `test3@example.com` | `test3`    | USER  |

Roles matter: an **ADMIN** can upload statements (on behalf of any user) and list users; a **USER** can only view their
own statements.

## API overview

| Method | Path                                 | Auth  | Description                                                        |
|--------|--------------------------------------|-------|-------------------------------------------------------------------|
| POST   | `/api/auth/v1/login`                 | none  | Authenticate (`{username, password}`) and receive an `idToken`    |
| POST   | `/api/statements/v1/upload-document` | ADMIN | Upload a PDF (multipart `file` + `username` = target user's email) |
| GET    | `/api/statements/v1/{documentId}`    | any   | Get a pre-signed download URL for one of your documents           |
| GET    | `/api/statements/v1/documents`       | any   | List your documents (paginated via `page`/`size`)                 |
| GET    | `/api/users/v1`                      | ADMIN | List users (paginated via `page`/`size`)                          |

Upload is **on behalf of** a user: the admin supplies the target user's email in the `username` field, and the document
is stored against *that* user — so it appears when *they* list their documents, not the admin.

## Testing the application

With `docker compose up --build` running, exercise the live API with Postman or `curl`.

### Postman collection

A ready-to-use collection lives at
[`postman/customer-statements.postman_collection.json`](postman/customer-statements.postman_collection.json). It has
five requests (**Login**, **Upload Document**, **Get Download Link**, **Get All Documents**, **List Users**) and a
collection-level bearer auth wired to a `token` variable, so you don't need to copy/paste JWTs.

To use it:

1. **Import** the file into Postman.
2. Requests target `{{baseUrl}}` (defaults to `http://localhost:8080`).
3. Run **Login** — its body defaults to `admin@example.com`/`password` (the other seeded users are included as commented
   JSON; swap them in to test as a different user). A script saves the returned `idToken` into the `token`
   collection variable, which every other request sends as `Authorization: Bearer {{token}}`.
4. Run **Upload Document**, attaching a PDF in the `file` field and setting `username` to the target user's email. Its
    script captures the returned `documentId`.
5. Run **Get Download Link** — it uses the captured `{{documentId}}` automatically.
6. Run **Get All Documents** to page through the logged-in user's statements, or **List Users** (admin only) to list
   users.

Because uploads are on behalf of a user, log in as that user (e.g. `test1@example.com`) to see the statement an admin
created for them.

### curl example

```bash
# Log in as the admin (username is the email) and capture the id token
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/v1/login \
  -H "Content-Type: application/json" \
  -d '{"username": "admin@example.com", "password": "password"}' | jq -r .idToken)

# Upload a statement on behalf of test1 (admin only; username = target user's email)
curl -X POST http://localhost:8080/api/statements/v1/upload-document \
  -H "Authorization: Bearer $TOKEN" \
  -F "file=@/path/to/statement.pdf" \
  -F "username=test1@example.com"

# List users (admin only)
curl "http://localhost:8080/api/users/v1?page=0&size=10" \
  -H "Authorization: Bearer $TOKEN"

# The document belongs to test1: log in as them to see it
T1=$(curl -s -X POST http://localhost:8080/api/auth/v1/login \
  -H "Content-Type: application/json" \
  -d '{"username": "test1@example.com", "password": "test1"}' | jq -r .idToken)

# List test1's documents, then get a pre-signed download link for one
curl "http://localhost:8080/api/statements/v1/documents?page=0&size=10" \
  -H "Authorization: Bearer $TOKEN"

curl http://localhost:8080/api/statements/v1/DOCUMENT_ID \
  -H "Authorization: Bearer $TOKEN"
```

[![My Skills](https://skillicons.dev/icons?i=aws,java,spring,git,redis,postgres,docker,postman)](https://skillicons.dev)