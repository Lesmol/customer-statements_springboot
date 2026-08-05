# Customer Account Statements Service

A Spring Boot service for uploading, storing, and retrieving customer account statement. Files are stored in AWS S3,
data is persisted on PostgreSQL, and pre-signed download URLs are cached in Redis. Access is protected with JWT-based
authentication.

## Prerequisites

- [Docker](https://docs.docker.com/get-docker/) and Docker Compose
- An AWS S3 bucket and credentials to access it — see [AWSSETUP.md](AWSSETUP.md) for how to set this up

## Configuration

The app is configured via environment variables, loaded by Docker Compose from a `.env` file in the project root. Create
one with:

```env
ACCESS_KEY=<your AWS access key id>
SECRET_KEY=<your AWS secret access key>
REGION=<your AWS region> [app defaults to af-south-1]
BUCKET_NAME=<your S3 bucket name>
```

See [AWSSETUP.md](AWSSETUP.md) for how to obtain `ACCESS_KEY`, `SECRET_KEY`, `REGION`, and `BUCKET_NAME`.

All other configuration (database, Redis, JWT secret) has working defaults for local use, defined in
`docker-compose.yaml` and `src/main/resources/application-local.yaml`.

## Running locally

From the project root:

```bash
docker compose up --build
```

This starts three containers:

| Service    | Description                                   | Port |
|------------|-----------------------------------------------|------|
| `app`      | The Spring Boot application (`local` profile) | 8080 |
| `postgres` | PostgreSQL 18 database                        | 5432 |
| `redis`    | Redis 8 cache                                 | 6379 |

Once running, the API is available at `http://localhost:8080`.

To stop everything:

```bash
docker compose down
```

## API overview

| Method | Path                                 | Description                             |
|--------|--------------------------------------|-----------------------------------------|
| POST   | `/api/auth/v1/login`                 | Authenticate and receive a JWT          |
| POST   | `/api/statements/v1/upload-document` | Upload a statement file (multipart)     |
| GET    | `/api/statements/v1/{documentId}`    | Get a pre-signed download URL           |
| GET    | `/api/statements/v1/documents`       | List the authenticated user's documents |

Requests to `/api/statements/**` require an `Authorization: Bearer <token>` header obtained from the login endpoint.

## Testing the application

With `docker compose up --build` running, you can exercise the live API manually, either with Postman or `curl`.

### Manual testing with seeded users

Once `docker compose up --build` is running, three users are seeded automatically by `UserSeeder` (`local` profile only), all with password `Test@123`:

- `user1`
- `user2`
- `user3`

Log in as any of them to obtain a JWT, then use that token to call the `/api/statements/**` endpoints.

### Postman collection

A ready-to-use Postman collection is available at [
`postman/customer-statements.postman_collection.json`](postman/customer-statements.postman_collection.json). It
contains four requests (**Login**, **Upload Document**, **Get Download Link**, and **Get All Documents**) and a
collection-level bearer auth wired to a `token` variable, so you don't need to copy/paste JWTs between requests.

To use it:

1. Download the file (or clone the repo) and open Postman.
2. **Import** → select the file.
3. Requests are pointed at `http://localhost:8080`. Run **Login** first; its body defaults to `user1`/`Test@123`
   (the requests for `user2` and `user3` are included as commented out JSON in the same body; swap them in to test
   as a different seeded user). A script on the request automatically saves the returned JWT into the `token`
   collection variable, which every other request sends via `Authorization: Bearer {{token}}`.
4. Run **Upload Document**, attaching a file in the `file` form field, to create a statement for the logged-in user.
5. Run **Get All Documents** to list that user's uploaded statements and copy a `documentId` from the response.
6. For **Get Download Link**, append the `documentId` to the URL (`/api/statements/v1/{documentId}`) to get a
   download link for that statement.

Auth is per user, logging in as a different seeded user and repeating steps 4 - 6 is a quick way to confirm
that users can only see their own documents.

### curl example

```bash
# Log in
curl -X POST http://localhost:8080/api/auth/v1/login \
  -H "Content-Type: application/json" \
  -d '{"username": "user1", "password": "Test@123"}'

# Upload a statement (replace TOKEN with the JWT from above)
curl -X POST http://localhost:8080/api/statements/v1/upload-document \
  -H "Authorization: Bearer TOKEN" \
  -F "file=@/path/to/statement.pdf"

# List your documents
curl http://localhost:8080/api/statements/v1/documents \
  -H "Authorization: Bearer TOKEN"

# Get a download link (replace DOCUMENT_ID with an id from the list above)
curl http://localhost:8080/api/statements/v1/DOCUMENT_ID \
  -H "Authorization: Bearer TOKEN"
```

[![My Skills](https://skillicons.dev/icons?i=aws,java,spring,git,redis,postgres,docker,postman)](https://skillicons.dev)