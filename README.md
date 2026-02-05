# Support Simulator (Spring Boot + Postgres)

A portfolio-friendly, enterprise-leaning backend focused on **debugging**, **CI/CD**, and **production support workflows**.

## Quickstart (local)

Prereqs:
- Java 21+
- (Optional) Docker Desktop for Postgres profile

### 1) Run the API (default: in-memory H2)

This starts the app with an in-memory H2 database, so you can run it without Docker.

```bash
./mvnw spring-boot:run
```

Windows PowerShell:

```powershell
.\mvnw.cmd spring-boot:run
```

Optional: H2 console is available at `http://localhost:8080/h2-console`.

### 2) Run the API with Postgres (Docker)

Start Postgres:

```bash
docker compose up -d
```

Then run with the `postgres` profile:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=postgres
```

Windows PowerShell:

```powershell
.\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=postgres
```

Note: This project maps the container's Postgres port 5432 to host port **5433** to avoid conflicts with locally installed Postgres.

API base URL: `http://localhost:8080`

### Endpoints
- `GET /api/health` -> basic health
- `GET /api/issues` -> list issues (seeded)

## Notes
- DB schema is managed with **Flyway** migrations in `src/main/resources/db/migration`.
- CI is configured in `.github/workflows/ci.yml`.

## Testing

```bash
./mvnw test
```

- By default, tests use an in-memory **H2** database (configured in `src/test/resources/application.yml`).
- The `IssueRepositoryTest` uses **Testcontainers** (real Postgres) and will auto-skip if Docker isn't available.

## Next step (later)
- Add MongoDB for an audit/event-log subsystem.
