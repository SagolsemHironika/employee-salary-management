# AI Tool Usage Log

This project was built with **Claude Code** (Anthropic's agentic CLI) in a single
continuous session, driven by the prompts below. Each entry is the actual
instruction given, in order, with what it produced.

## 1. Initial project plan
> Build employee salary management software for an organization with 10,000
> employees. [Full assessment brief: HR Manager persona, Java/Spring Boot or
> similar backend, ReactJS/NextJS or AngularJS-with-Java frontend, SQLite or
> other relational DB, 10k-employee seed, requirements doc, deployed +
> demoable.]

Produced: the one-page requirements document (`REQUIREMENTS.md`), the
architecture decisions (Spring Boot + Angular + Postgres, layered backend,
effective-dated salary history as the core data-modeling decision), and the
initial project scaffold.

## 2. "I want to deploy this software and see the UI locally"
Clarified scope into a "walking skeleton" first: Postgres via Docker, a
minimal `Employee` entity/API with a handful of hand-written seed rows, and
an Angular page listing employees — deferring the full feature set until
something was visibly working end-to-end.

Produced: `docker-compose.yml`, the `employee` package (entity, repository,
service, controller), a temporary permit-all `SecurityConfig`, and the
Angular employee list page with Material.

## 3. "we need to use angular .js for front end"
Clarifying question revealed this meant "Angular" (already in use), not the
unmaintained AngularJS 1.x — confirmed no change was needed.

## 4. The full assessment brief, pasted verbatim, with: "make the software
more realistic, follow this instructions"
This was the instruction that drove the bulk of the build. Planned and
executed as nine phases:

1. Retroactive incremental git commits for the walking skeleton
2. Salary record history API — the core differentiator, with the
   effective-dated transition logic (close the previous record, open a new
   one, atomically)
3. JWT authentication (login endpoint, stateless filter, bootstrapped admin
   account)
4. 10,000-employee Faker-based seed generator, reproducible via a fixed
   random seed
5. Analytics endpoints (headcount/payroll by country/department, salary
   distribution) — SQL-side aggregation, not pulled into the JVM
6. Backend unit tests (Mockito) + Testcontainers integration tests
7. Frontend: auth flow, employee filters, salary history UI, analytics
   dashboard
8. Frontend unit tests (Vitest + Angular TestBed)
9. Deployment prep (Docker images, prod compose, CI) and this artifact set

Follow-up clarifying questions during this phase (asked by Claude, answered
by the user) fixed scope decisions explicitly rather than assuming them:
whether to actually deploy to external providers now or just prepare for it
(chose: prepare now, deploy later, since deployment requires the user's own
hosting accounts), and whether to build the full brief or just the core
feature (chose: build it all).

## 5. "don't commit yet"
Held all git commits pending explicit go-ahead, while continuing
implementation work — a deliberate separation between "build it" and
"commit it" so the user could review before the history was written.

## Notable mid-build corrections
A few points where the initial approach was wrong and had to be fixed —
worth recording because they reflect real engineering judgment calls, not
just prompt-following:

- **Testcontainers container lifecycle**: the first integration-test setup
  used `@Testcontainers`/`@Container` on a shared static field in an
  abstract base class. That combination stops the container after the
  first subclass's tests finish, breaking every subclass that runs after
  it. Fixed by switching to Testcontainers' documented "singleton
  container" pattern — start it once in a static initializer, no JUnit
  lifecycle management.
- **Test isolation across `@BeforeEach` calls**: without `@Transactional`
  on the integration test base, fixture data inserted in `@BeforeEach`
  accumulated across test methods (and across test classes, since they
  share one context), causing exact-count assertions to fail
  nondeterministically depending on execution order. Fixed by wrapping the
  integration test base in `@Transactional` so each test rolls back
  cleanly.
- **Package churn in this Spring Boot generation**: several classes moved
  packages from what's documented for older Spring Boot versions
  (`UserDetailsServiceAutoConfiguration`, `AutoConfigureMockMvc`) and the
  JSON library changed from `com.fasterxml.jackson` to `tools.jackson`
  (Jackson 3). Each was diagnosed by inspecting the actual jars in the
  local Maven repository rather than guessing from memory.
