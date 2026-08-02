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

### Seeded users

On startup (`local` profile only), three users are seeded for testing, all with password `Test@123`:

- `user1`
- `user2`
- `user3`

## API overview

| Method | Path                                 | Description                             |
|--------|--------------------------------------|-----------------------------------------|
| POST   | `/api/auth/v1/login`                 | Authenticate and receive a JWT          |
| POST   | `/api/statements/v1/upload-document` | Upload a statement file (multipart)     |
| GET    | `/api/statements/v1/{documentId}`    | Get a pre-signed download URL           |
| GET    | `/api/statements/v1/documents`       | List the authenticated user's documents |

Requests to `/api/statements/**` require an `Authorization: Bearer <token>` header obtained from the login endpoint.

### Postman collection

A ready-to-use Postman collection is available at [
`postman/customer-statements.postman_collection.json`](postman/customer-statements.postman_collection.json). To use it:

1. Download the file (or clone the repo) and open Postman.
2. **Import** → select the file.
3. Requests are pointed at `http://localhost:8080`. Run **Login** first; it stores the returned JWT in the `token`
   collection variable, which the other requests send automatically via the collection-level bearer auth.
4. For **Get Download Link**, append a document ID to the URL (e.g. `.../api/statements/v1/{documentId}`); use the ID
   returned by **Upload Document** or one of the `documentId's` returned by **Get All Documents**.

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
```

[![My Skills](https://skillicons.dev/icons?i=aws,java,spring,git,redis,postgres,docker,postman)](https://skillicons.dev)