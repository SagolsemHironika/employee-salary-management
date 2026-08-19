# Employee Salary Management Software

An HR system for managing salary data across 10,000 employees in multiple
countries — built for ACME's HR team as a replacement for spreadsheets, with
the ability to answer aggregate compensation questions.

See [`docs/REQUIREMENTS.md`](docs/REQUIREMENTS.md) for scope and what was
deliberately left out, and [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) for
the design decisions and trade-offs.

## Stack
Java 21 + Spring Boot · PostgreSQL · Angular + Angular Material · Docker

## Quick start (local)

```
docker compose up -d                                    # Postgres
cd salary && ./mvnw spring-boot:run                      # API on :8080
cd frontend && npx ng serve                               # UI on :4200
```

Open http://localhost:4200. Default login: `admin@acme.example` /
`ChangeMe123!` (bootstrapped automatically on first run).

To load the full 10,000-employee dataset instead of the small dev fixture:

```
cd salary && ./mvnw spring-boot:run -Dspring-boot.run.profiles=seed
```

Full instructions — including the production Docker Compose path and actual
cloud deployment — are in [`docs/DEPLOYMENT.md`](docs/DEPLOYMENT.md).

## Testing

```
cd salary && ./mvnw test          # unit tests (Mockito, no DB, ~seconds)
cd salary && ./mvnw verify         # + integration tests (Testcontainers Postgres)
cd frontend && npx ng test         # frontend unit tests (Vitest)
```

## Docs
- [`docs/REQUIREMENTS.md`](docs/REQUIREMENTS.md) — one-page requirements, scope, and what's out
- [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) — architecture diagram and trade-offs
- [`docs/PERFORMANCE.md`](docs/PERFORMANCE.md) — pagination, SQL-side aggregation, indexing, seed-script batching trade-off
- [`docs/DEPLOYMENT.md`](docs/DEPLOYMENT.md) — local Docker dry run and real cloud deployment
- [`docs/DEMO_SCRIPT.md`](docs/DEMO_SCRIPT.md) — walkthrough script for the demo video
- [`docs/ai-prompts.md`](docs/ai-prompts.md) — log of the prompts used to build this with Claude Code
