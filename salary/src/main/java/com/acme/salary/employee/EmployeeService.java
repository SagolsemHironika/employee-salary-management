package com.acme.salary.employee;

import com.acme.salary.common.PagedResponse;
import com.acme.salary.common.ResourceNotFoundException;
import com.acme.salary.salary.SalaryRecord;
import com.acme.salary.salary.SalaryRecordRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class EmployeeService {

    // Sort request properties are validated against this whitelist before
    // ever reaching the SQL string, so there is no injection risk despite
    // the column name being concatenated in below.
    private static final Map<String, String> SORTABLE_COLUMNS =
            Map.of("employeeCode", "e.employee_code", "salary", "current_usd_salary");

    private static final String SELECT_COLUMNS =
            """
            SELECT e.id, e.employee_code, e.first_name, e.last_name, e.email, e.country_code, e.department,
                   e.job_title, e.band, e.hire_date, e.status,
                   sr.base_salary AS current_base_salary, sr.currency_code AS current_currency_code,
                   (sr.base_salary * fx.rate_to_usd) AS current_usd_salary
            """;

    // Employees are LEFT JOINed to their current (end_date IS NULL) salary
    // record, normalized to USD via the latest fx_rates_snapshot row per
    // currency - the same pattern AnalyticsService uses - so sorting by
    // "salary" compares like-for-like across currencies rather than raw,
    // incomparable local-currency numbers.
    private static final String FROM_CLAUSE =
            """
            FROM employees e
            LEFT JOIN salary_records sr ON sr.employee_id = e.id AND sr.end_date IS NULL
            LEFT JOIN LATERAL (
                SELECT rate_to_usd FROM fx_rates_snapshot f
                WHERE f.currency_code = sr.currency_code
                ORDER BY f.as_of_date DESC LIMIT 1
            ) fx ON true
            """;

    private final EmployeeRepository employeeRepository;
    private final SalaryRecordRepository salaryRecordRepository;
    private final JdbcTemplate jdbcTemplate;

    public EmployeeService(
            EmployeeRepository employeeRepository,
            SalaryRecordRepository salaryRecordRepository,
            JdbcTemplate jdbcTemplate) {
        this.employeeRepository = employeeRepository;
        this.salaryRecordRepository = salaryRecordRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    public PagedResponse<EmployeeSummary> list(String countryCode, String department, Pageable pageable) {
        String country = StringUtils.hasText(countryCode) ? countryCode : null;
        String dept = StringUtils.hasText(department) ? department : null;

        StringBuilder whereClause = new StringBuilder(" WHERE 1 = 1");
        List<Object> filterParams = new ArrayList<>();
        if (country != null) {
            whereClause.append(" AND e.country_code = ?");
            filterParams.add(country);
        }
        if (dept != null) {
            whereClause.append(" AND e.department = ?");
            filterParams.add(dept);
        }

        String fromAndFilter = FROM_CLAUSE + whereClause;

        long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) " + fromAndFilter, Long.class, filterParams.toArray());

        List<Object> selectParams = new ArrayList<>(filterParams);
        selectParams.add(pageable.getPageSize());
        selectParams.add(pageable.getOffset());

        String selectQuery =
                SELECT_COLUMNS + fromAndFilter + orderByClause(pageable.getSort()) + " LIMIT ? OFFSET ?";

        List<EmployeeSummary> items = jdbcTemplate.query(
                selectQuery,
                (rs, rowNum) -> new EmployeeSummary(
                        rs.getLong("id"),
                        rs.getString("employee_code"),
                        rs.getString("first_name"),
                        rs.getString("last_name"),
                        rs.getString("email"),
                        rs.getString("country_code"),
                        rs.getString("department"),
                        rs.getString("job_title"),
                        rs.getString("band"),
                        rs.getObject("hire_date", LocalDate.class),
                        rs.getString("status"),
                        rs.getBigDecimal("current_base_salary"),
                        rs.getString("current_currency_code")),
                selectParams.toArray());

        return new PagedResponse<>(items, total, pageable.getPageNumber(), pageable.getPageSize());
    }

    public EmployeeSummary get(Long id) {
        Employee employee = employeeRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee " + id + " not found"));
        SalaryRecord currentSalary =
                salaryRecordRepository.findByEmployeeIdAndEndDateIsNull(id).orElse(null);
        return EmployeeSummary.from(employee, currentSalary);
    }

    private String orderByClause(Sort sort) {
        if (sort.isUnsorted()) {
            // Deliberately not e.id: employee_code is assigned in the same
            // sequence as id, so an id-based default would make the first
            // "Employee ID" ascending sort click look like a no-op (it
            // would exactly match this default order). Name ordering has
            // no relationship to id/employee_code, so every explicit sort
            // - including that first click - visibly changes row order.
            return " ORDER BY e.last_name ASC, e.first_name ASC ";
        }
        Sort.Order order = sort.iterator().next();
        String column = SORTABLE_COLUMNS.getOrDefault(order.getProperty(), "e.id");
        String direction = order.isAscending() ? "ASC" : "DESC";
        String nullHandling = column.equals("current_usd_salary") ? " NULLS LAST" : "";
        return " ORDER BY " + column + " " + direction + nullHandling + " ";
    }
}
