# Demo Video Script

A ~4-5 minute walkthrough covering the requirements brief end to end. Record
against either the local Docker stack (`docker-compose.prod.yml`) or a real
deployment.

This file is the **shot list** — what to put on screen, in what order, with
timings. For the spoken narration that goes over it, see
[`DEMO_NARRATION.md`](DEMO_NARRATION.md).

## 1. Requirements framing (30s)
Briefly state the problem: HR at ACME manages 10,000 employees' salaries
across countries via spreadsheets; this replaces that with a system that can
answer "how do we pay people" questions. Point at `docs/REQUIREMENTS.md`.

## 2. Login (15s)
Show the login page, sign in as the HR admin. Note that every route redirects
here when unauthenticated (open a protected URL in an incognito tab to show
the 401/redirect).

## 3. Employee directory (60s)
- Show the paginated list (10,000 employees, 20 per page).
- Filter by country (e.g. `IN`) and department (e.g. `Engineering`) —
  point out this is a real server-side query, not client-side filtering of
  everything.
- Click into one employee.

## 4. Salary history — the core feature (90s)
- On the employee detail page, show the current salary and the history
  table underneath it.
- Record a new salary change (a raise, with a later effective date) and show
  the table update: the previous record now has an end date, the new one is
  "current."
- Try to submit a change with an effective date *before* the current
  record's — show the validation error. This is the one piece of real
  business logic in the system (`SalaryRecordService.createRecord`), and
  it's directly unit-tested (`SalaryRecordServiceTest`).

## 5. Analytics dashboard (60s)
- Headcount and total payroll (USD-normalized) at the top.
- Headcount by department and payroll by country charts — point out these
  answer the literal brief ask ("answer questions about how the org pays
  people").
- Salary distribution chart.

## 6. Engineering quality, briefly (45s)
- `./mvnw test` — fast unit tests, no database.
- `./mvnw verify` — Testcontainers integration tests against a real
  Postgres, including the exact-aggregate analytics test.
- `npx ng test` — frontend tests.
- Show the git log — incremental commits, not one dump.

## 7. Close (15s)
Point at `docs/ARCHITECTURE.md` for the "why," and `docs/REQUIREMENTS.md`
for what was deliberately left out and why.
