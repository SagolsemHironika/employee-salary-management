# Deployment Guide

This covers two paths: a local production-shaped dry run (no external
accounts needed), and an actual public deployment (needs your own hosting
accounts — nothing here was run on your behalf).

## Local dry run (production Docker images, no cloud accounts)

```
docker compose -f docker-compose.prod.yml up -d --build
```

This builds and runs three containers on one Docker network: Postgres, the
Spring Boot API (multi-stage build, `salary/Dockerfile`), and the Angular app
served by nginx (multi-stage build, `frontend/Dockerfile`, with nginx
reverse-proxying `/employees`, `/analytics`, `/auth` to the API container so
the SPA's relative API calls work unchanged).

- App: http://localhost:8081
- API directly: http://localhost:8080
- Default login: `admin@acme.example` / `ChangeMe123!` (bootstrapped on first
  run by `AdminUserInitializer` — override via the `ADMIN_EMAIL`/
  `ADMIN_PASSWORD` environment variables before first run)
- To seed the full 10,000-employee dataset in this stack, run the backend
  container once with `SPRING_PROFILES_ACTIVE=seed` (see the seeding section
  below) instead of the default profile.

Tear down: `docker compose -f docker-compose.prod.yml down` (add `-v` to also
drop the Postgres volume).

## Seeding 10,000 employees

The seed generator (`SeedRunner`) only runs under the `seed` Spring profile,
against whichever Postgres the app is pointed at:

```
cd salary
./mvnw spring-boot:run -Dspring-boot.run.profiles=seed
```

It's idempotent-guarded (skips if any employees already exist), reproducible
(fixed random seed), and takes about 15 seconds locally. Run it once against
whichever database backs your deployment.

## Actual cloud deployment

Pick your own providers — this is written against Railway (API) + Neon
(Postgres) + Vercel (frontend) as one concrete path, but Render/Fly.io/
Supabase/Netlify are equally fine substitutes using the same Dockerfiles.

### 1. Database — Neon (or Supabase/Railway Postgres)
1. Create a free Postgres project. Note the connection string (host, port,
   database, user, password).
2. Nothing else to do here — Flyway (`salary/src/main/resources/db/migration`)
   creates the schema automatically the first time the API starts.

### 2. API — Railway (or Render/Fly.io)
1. New service from this repo, root directory `salary/`. Railway/Render both
   auto-detect the `Dockerfile` in that directory.
2. Set environment variables:
   - `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD` — from step 1
   - `JWT_SECRET` — a real random 32+ byte string (`openssl rand -base64 32`),
     not the `application.yml` dev default
   - `ADMIN_EMAIL` / `ADMIN_PASSWORD` — your real admin credentials
3. Deploy. On first boot, Flyway migrates the schema and
   `AdminUserInitializer` creates the admin account.
4. Run the seed step once (a one-off command/job on the same service, same
   environment variables, with `SPRING_PROFILES_ACTIVE=seed` added).

### 3. Frontend — Vercel (or Netlify)
1. New project from this repo, root directory `frontend/`.
2. Build command: `ng build`; output directory: `dist/frontend/browser`.
3. Since Vercel/Netlify serve static files (no nginx reverse proxy like the
   local Docker path), set the API base URL via environment config rather
   than relying on relative paths — see "Note on the API URL" below.
4. Deploy.

### Note on the API URL
Locally (`ng serve` and the local Docker Compose path) the frontend calls
relative paths (`/employees`, `/auth/login`, ...) which are proxied to the
backend (`proxy.conf.json` in dev, nginx in the prod Docker Compose). A
static host like Vercel has no proxy layer, so the deployed API's real URL
needs to reach the browser. The straightforward fix: add an
`environment.ts`/`environment.prod.ts` pair with an `apiBaseUrl`, prefix
API calls in the Angular services (`EmployeeService`, `SalaryRecordService`,
`AnalyticsService`, `AuthService`) with it, and enable CORS on the backend
for the deployed frontend's origin (`SecurityConfig` currently has no CORS
configuration since dev and the local Docker path never needed
cross-origin requests). This is the one piece of environment-specific
wiring intentionally left for whoever actually deploys, since it depends on
the real chosen domain names.

## Demo video
See `docs/DEMO_SCRIPT.md` for a walkthrough script once something is
running (local Docker or deployed).
