# Demo Narration — spoken script

The words to say on camera, with screen cues inline. For the shot list and
timings, see [`DEMO_SCRIPT.md`](DEMO_SCRIPT.md) — that one is *what to show*,
this one is *what to say*.

- **Length**: 761 spoken words — **~5 min 00 s** at a measured 150 wpm. Most
  people run 160–170 wpm on camera, which lands this nearer 4 min 30 s.
- **Audience**: technical evaluator who has not read the code
- **Rule applied throughout**: every claim is something visible on screen or
  present in the repo. No adjectives standing in for evidence.
- **If you run long**, cut in this order: the scope list in §2 down to two
  items, then the analytics beat in §4. Do not cut the append-only
  explanation in §3 or the measurement admission in §5 — those are the two
  parts that carry the most signal.

## Before recording

- [ ] Backend up (`cd salary && ./mvnw spring-boot:run`) — Flyway must be at **V4**
- [ ] Frontend up (`cd frontend && npm start`)
- [ ] Signed out, so the login step is real
- [ ] A terminal ready for the `EXPLAIN ANALYZE` and test-run shots
- [ ] Pick the employee to open ahead of time so you are not scrolling on camera
- [ ] Decide light or dark theme and stay in it

---

## 1 · Problem framing · ~25 s

**[SCREEN: `docs/REQUIREMENTS.md`]**

"ACME's HR team manages salaries for about ten thousand employees across
several countries, and today that lives in spreadsheets. The real problem
isn't storage — it's that a spreadsheet can't reliably answer *what does
Engineering cost us in India*, or *what was this person earning eighteen
months ago*. So: one system of record for salary data, that answers aggregate
questions and keeps a trustworthy history of every change."

## 2 · Scope decisions · ~40 s

**[SCREEN: scroll to the "Explicitly Out of Scope" table]**

"I wrote the scope down before writing code, and these were judgment calls,
not things I ran out of time for.

No payroll processing — tax and disbursement is a different regulatory domain
per country, and says nothing about how I model salary data. No self-service
portal and no SSO — both are a second auth model for no extra signal. No
approval workflows: a workflow-engine problem, not a salary-data problem, and
the natural v2. And no live FX — a static rate snapshot, so nothing external
can fail mid-demo.

Each one's in the doc with the reasoning next to it."

## 3 · Architecture · ~75 s

**[SCREEN: `docs/ARCHITECTURE.md`, then the backend package tree]**

"It's a three-tier monolith — Angular, Spring Boot, Postgres. For one team and
one org, splitting into services would add network calls and deployment
complexity to buy nothing.

The brief allowed SQLite. I chose Postgres because SQLite takes a single
writer at a time, which is wrong for concurrent HR users; because money needs
exact decimal types, not floating point; and because the analytics lean on
CTEs."

**[SCREEN: `V1__init_schema.sql`, `salary_records` table]**

"The key modeling decision is here. Salary history is append-only and
effective-dated — when someone gets a raise, I don't update a number in place.
I write a new row with a start date and close the previous one with an end
date. The current salary is just the row with no end date.

That does a lot of work: an audit trail by construction rather than bolted on,
and you can ask what someone earned at any past date. With an overwrite model,
that's simply gone."

**[SCREEN: `AnalyticsService.java`]**

"And analytics runs as SQL — one query per endpoint, grouped in the database.
I never pull rows into Java to sum them. At ten thousand rows either works;
this one doesn't change when the table gets much bigger."

## 4 · Live walkthrough · ~75 s

**[SCREEN: login page → sign in]**

"Signing in as the HR admin. Every route is behind auth, and an expired token
sends you back here rather than leaving you on a page that looks logged in but
shows nothing."

**[SCREEN: employee list, 10,000 records]**

"The directory — ten thousand seeded employees, twenty per page. Filtering by
country and department is a server-side query, not client-side filtering of a
list I already downloaded, and page size is capped."

**[SCREEN: point at the salary column, scroll a few rows]**

"One deliberate detail: salaries are right-aligned in tabular figures — digits
of equal width — so the column stays scannable across mixed currencies, and
the currency code is always shown, because comparing raw local amounts is
misleading."

**[SCREEN: click into an employee → salary history timeline]**

"The history view — each entry shows the period, the amount, and the change
against the previous one. Notice the percentage is suppressed where the
currency changed: comparing across currencies would produce a meaningless
number."

**[SCREEN: submit a raise with a later effective date]**

"I'll record a raise. The previous record now has an end date, the new one is
current — nothing was overwritten."

**[SCREEN: submit a backdated effective date, show the error]**

"And a date before the current record is rejected."

**[SCREEN: analytics dashboard]**

"Headcount and payroll by department and country, normalised to USD, plus
salary distribution. Ranked bars run horizontally so long department names
stay readable."

## 5 · Engineering quality · ~45 s

**[SCREEN: `./mvnw verify` output, then `AbstractIntegrationTest.java`]**

"Thirty-one backend tests. The integration ones run against a real Postgres in
Testcontainers, not H2 — because H2 isn't Postgres, and I'd rather catch a
dialect problem in CI than in production."

**[SCREEN: `EXPLAIN ANALYZE` before/after]**

"On performance I'll be straight with you: my own notes claimed this holds to
a hundred thousand rows unchanged. The query plans said otherwise — the
default list sorts by last name, and I'd never indexed those columns, so it
scanned and sorted all ten thousand rows. One index took it from twenty
milliseconds to under one."

**[SCREEN: `V4` migration]**

"The same migration makes 'one current salary per employee' a database rule
instead of a convention — and that caught a real bug the moment I added it."

## 6 · AI usage & process · ~20 s

**[SCREEN: `docs/ai-prompts.md`]**

"I built this with Claude Code, and there's a log of every prompt I gave it in
the repo. It did scaffolding and iteration fast. The calls that mattered —
Postgres over SQLite, effective-dated history, what stayed out of v1 — were
mine, and when the tool's output disagreed with what I wanted, I changed the
output."

## 7 · Close · ~10 s

"v2 is approval workflows and live FX. But the foundation I'd defend is the
data model: because history is append-only, every question I haven't thought
of yet is still answerable."

---

## Likely follow-up questions

Short, grounded answers. Each is backed by something in the repo.

**"What happens if two HR users save a raise for the same person at once?"**
The database rejects the second one. There's a partial unique index on
`salary_records (employee_id) WHERE end_date IS NULL`, so at most one open
record can exist. The losing request gets a 409 telling it to reload and
reapply — not a corrupted history. `@Transactional` alone would not have
prevented it, because under READ COMMITTED both requests can read the same
open record before either writes.

**"Does this hold at 100k employees?"**
The list does, now — it's an index scan, so it's flat in table size. Analytics
is the real ceiling: each endpoint is a full aggregation over active
employees, roughly linear, and the dashboard fires four of them. The fix there
is caching rather than query tuning, because the results are identical for
every user and only change when a salary record is written. That's written up
in `docs/PERFORMANCE.md`.

**"Why Testcontainers instead of H2? It's slower."**
Because H2 isn't Postgres. The analytics queries use CTEs, `DISTINCT ON` and a
partial index — H2 either behaves differently or doesn't support them, so a
green H2 suite would tell me nothing about whether production works.

**"Why no employee self-service / approvals / SSO?"**
Scope decisions, documented before I started, in `REQUIREMENTS.md` with the
reasoning per row. Each is a different problem domain rather than more of this
one; approvals is the natural v2.

**"How much of this did the AI write?"**
Most of the typing, very little of the deciding. `docs/ai-prompts.md` has the
actual prompt log. The architecture, the data model, and the scope boundaries
were my calls — and where measurement contradicted my assumptions, like the
missing index, I changed the code rather than the claim.
