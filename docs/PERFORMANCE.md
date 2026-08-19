# Performance Notes

## Server-side pagination everywhere
`GET /employees` never returns the full table — it's `Pageable`-backed
(`EmployeeController` / `EmployeeService`), defaulting to 20 rows. A naive
"return all 10,000" endpoint works fine in local dev and is a real production
bug; pagination was there from the first commit, not retrofitted.

## Aggregation runs in SQL, not in the JVM
`AnalyticsService` never pulls employee/salary rows into Java to sum or
group them. Each endpoint is one SQL query (a `WITH` CTE joining employees'
current salary record to the latest FX rate, then `GROUP BY`/`CASE WHEN` in
Postgres). At 10,000 rows this wouldn't matter much either way, but it's the
right default: it's the same query regardless of whether the table has 10
thousand or 10 million rows, and it's what a DBA would expect to see.

## Indexes present from the first migration
`V2__seed_indexes.sql` adds `employees(country_code, department)` and
`salary_records(employee_id, effective_date)` before any data exists, so the
query plans used by `/employees` filtering and `/employees/{id}/salary-records`
were never accidentally validated against an unindexed table locally and
then discovered to be slow later.

## Seed script: single-row inserts, not JDBC batching — a deliberate trade-off
`hibernate.jdbc.batch_size=500` is configured in `application.yml`, but it
doesn't actually apply to the seed run: Hibernate disables JDBC batching for
entities using `GenerationType.IDENTITY` (our `@GeneratedValue` strategy,
backed by Postgres `BIGSERIAL`), because each insert's generated ID has to be
read back before the next row can be prepared. Getting real batching would
mean switching to a pooled `SEQUENCE` generator — more moving parts in the
schema for a script that runs once, offline, to populate a demo dataset.

In practice this doesn't matter at this scale: seeding 10,000 employees plus
~15,000 salary records (single-row inserts, one transaction per 500-row
chunk) completes in about 15 seconds locally. The `batch_size` setting is
kept because it's harmless and correct for any future bulk *update* path;
it's simply not the reason the seed script is fast.

## What would need to change past 10k
None of the above architecture changes for 100k or 1M employees — the same
indexes and SQL-side aggregation keep working. The one thing that would
start to matter is the `IDENTITY`-vs-`SEQUENCE` trade-off above, if seeding
needed to run in seconds rather than tens of seconds; not a concern at the
scale this system was built for.
