-- Supports the default employee listing order.
--
-- EmployeeService.orderByClause falls back to ORDER BY last_name, first_name
-- when no explicit sort is requested, which is what the directory loads with.
-- Those columns were never indexed, so EXPLAIN ANALYZE showed a sequential
-- scan over every employee followed by a top-N heapsort just to return 20
-- rows -- 20.4ms at 10k rows, and linear in table size from there.
--
-- With this index the same query plans as an index scan at 0.85ms, and stays
-- flat as the table grows. The trailing id is not decoration: it matches the
-- pagination tiebreaker the service appends, so the index covers the whole
-- ORDER BY rather than only its leading columns.
CREATE INDEX idx_employees_name ON employees (last_name, first_name, id);

-- Enforces "at most one open salary record per employee".
--
-- SalaryRecordService.createRecord reads the current open record, closes it,
-- then inserts its replacement. @Transactional does not make that sequence
-- atomic under READ COMMITTED, which is PostgreSQL's default: two concurrent
-- requests for the same employee can both read the same open record, both
-- close it, and both insert. The employee then has two "current" salaries.
--
-- That is not merely untidy. The employee list LEFT JOINs to the open record,
-- so a duplicate silently doubles that employee's row in the directory and
-- inflates the reported total, and "current salary" stops having one answer.
--
-- A partial unique index states the invariant once, in the place that every
-- write path goes through -- service code, bulk import, or manual SQL -- and
-- lets PostgreSQL reject the second insert rather than trusting callers to
-- serialize themselves.
CREATE UNIQUE INDEX uq_salary_records_open_per_employee
    ON salary_records (employee_id)
    WHERE end_date IS NULL;
