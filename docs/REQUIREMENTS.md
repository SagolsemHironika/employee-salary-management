# Employee Salary Management System — Requirements

## Goal
Give HR Managers a single system of record for salary data across 10,000
employees in multiple countries, replacing spreadsheets, with the ability
to answer aggregate compensation questions.

## Persona
Single role: HR Manager / Admin. No employee self-service, no multi-role
approval chains (v1 scoping decision — see Out of Scope).

## In Scope (v1)
- Employee directory: search, filter, paginate across 10k records
- Salary records with **history** — every change is a new dated row, not
  an overwrite. Enables trend analysis and an audit trail by construction.
- Multi-country support: currency per employee, country/department as
  first-class dimensions
- Analytics: headcount & payroll cost by country/department, salary
  distribution, band analysis
- CSV/programmatic seed of 10,000 realistic employees
- Basic auth (single HR Admin role, JWT)

## Explicitly Out of Scope (v1)
| Item | Why excluded |
|---|---|
| Payroll processing / tax / disbursement | Different regulatory domain per country; months of work, not what's being assessed. |
| Employee self-service portal | Different auth model/UX; doubles scope for no extra signal on the core ask. |
| Multi-level approval workflows | A workflow-engine problem, not a salary-data-modeling problem. Natural v2. |
| Live FX conversion | Static FX snapshot table used instead of a live rates API. Avoids an external dependency/failure mode; documented limitation. |
| SSO / enterprise auth | Email+password JWT is enough to demonstrate auth correctness; SSO is infra, not product logic. |
| Multi-tenant | Single org, per the problem statement. |

## Non-Functional Requirements
- **Security**: salary data is sensitive PII. All endpoints behind auth.
  Never log salary values.
- **Performance**: server-side pagination everywhere (no unbounded list
  endpoints). Aggregations run in SQL, not pulled into app memory.
- **Auditability**: salary history is append-only; nothing is destructively
  overwritten.
- **Reproducibility**: seed data generation uses a fixed random seed.

## Acceptance Criteria (high level)
- Can create/search/filter/paginate employees across 10k+ records without
  full-table scans (verify via query plans on `country_code`,
  `department`, `email` indexes).
- Creating a new salary record closes the previous one's `end_date`
  atomically — no overlapping "current" records for one employee.
- Analytics endpoints return correct aggregates against seeded data,
  verified by integration tests with known expected totals.
- Unauthenticated requests to any `/employees`, `/analytics`, or
  `/employees/*/salary-records` route return 401.