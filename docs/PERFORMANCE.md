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

## The index that was missing — and how it was found
Having indexes for the *filtered* paths did not mean the default path was
covered, and assuming so was wrong. `EmployeeService.orderByClause` falls back
to `ORDER BY last_name, first_name` when no sort is requested — which is what
the directory loads with — and those columns were not indexed. `EXPLAIN
ANALYZE` against the seeded 10k dataset:

| | plan | time |
|---|---|---|
| before | `Seq Scan` employees + `Seq Scan` salary_records → top-N heapsort | **20.4 ms** |
| after `V4` | `Index Scan using idx_employees_name` | **0.74 ms** |

The time is the smaller point; the plan is the real one. A sequential scan
plus sort is linear in table size, so it degrades to roughly 200 ms at 100k
rows, while an index scan stays flat. `V4__list_performance_and_open_record_integrity.sql`
adds `employees(last_name, first_name, id)`.

The lesson worth keeping: this was found by reading query plans, not by
reasoning about which indexes "should" be enough.

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
The shape of the architecture holds — server-side pagination and SQL-side
aggregation are the same code at 100k or 1M. What does *not* automatically
hold is the assumption that the existing indexes cover every access path;
see above for a case where they did not, caught only by reading a query plan.

Measured or identified so far, in priority order:

1. **Analytics is the real ceiling.** Each of the three endpoints is a full
   aggregation over active employees (~11–13 ms at 10k), and the dashboard
   fires four requests on load. That is linear, so ~120 ms each at 100k,
   multiplied by concurrent users. The fix is caching, not query tuning:
   the results are identical for every user and only change when a salary
   record is written. A 60-second in-memory cache with eviction on write
   turns O(users × 4 scans) into O(1 scan/minute). Redis only becomes
   justified with more than one app instance.
2. **The list `COUNT(*)` joins tables it does not filter on** —
   8.8 ms versus 1.1 ms counting from `employees` alone. Safe to simplify
   now that `V4` guarantees the join cannot multiply rows.
3. **Deep `OFFSET` paging** would need keyset pagination, but only if users
   actually page thousands deep rather than filtering.

Explicitly *not* worth doing, having measured it: rewriting the FX `LATERAL`
join as a CTE. It looks like a per-row subquery, but PostgreSQL memoizes it
(9,992 cache hits against 8 misses on the 10k set), so the rewrite moved
12.7 ms to 11.0 ms — noise, in exchange for less obvious SQL.
