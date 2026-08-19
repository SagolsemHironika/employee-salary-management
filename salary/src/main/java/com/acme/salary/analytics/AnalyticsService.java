package com.acme.salary.analytics;

import com.acme.salary.common.InvalidRequestException;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class AnalyticsService {

    // Active employees' current (end_date IS NULL) salary, normalized to USD
    // using the latest fx_rates_snapshot row per currency. Aggregation runs
    // entirely in SQL rather than pulling rows into the JVM.
    private static final String CURRENT_USD_SALARY_CTE =
            """
            WITH current_usd_salary AS (
                SELECT e.id AS employee_id, e.country_code, e.department, e.band,
                       sr.base_salary * fx.rate_to_usd AS usd_salary
                FROM employees e
                JOIN salary_records sr ON sr.employee_id = e.id AND sr.end_date IS NULL
                JOIN LATERAL (
                    SELECT rate_to_usd FROM fx_rates_snapshot f
                    WHERE f.currency_code = sr.currency_code
                    ORDER BY f.as_of_date DESC LIMIT 1
                ) fx ON true
                WHERE e.status = 'active'
            )
            """;

    private static final Map<String, String> DIMENSION_COLUMNS =
            Map.of("country", "country_code", "department", "department", "band", "band");

    private final JdbcTemplate jdbcTemplate;

    public AnalyticsService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public AnalyticsSummary summary() {
        String sql = CURRENT_USD_SALARY_CTE
                + "SELECT COUNT(*) AS headcount, COALESCE(SUM(usd_salary), 0) AS total_payroll_usd "
                + "FROM current_usd_salary";
        return jdbcTemplate.queryForObject(
                sql,
                (rs, rowNum) ->
                        new AnalyticsSummary(rs.getLong("headcount"), rs.getBigDecimal("total_payroll_usd")));
    }

    public List<DimensionBreakdown> byDimension(String groupBy) {
        String column = DIMENSION_COLUMNS.get(groupBy);
        if (column == null) {
            throw new InvalidRequestException(
                    "groupBy must be one of " + DIMENSION_COLUMNS.keySet() + ", got '" + groupBy + "'");
        }
        String sql = CURRENT_USD_SALARY_CTE
                + "SELECT " + column + " AS dimension, COUNT(*) AS headcount, "
                + "COALESCE(SUM(usd_salary), 0) AS total_payroll_usd "
                + "FROM current_usd_salary GROUP BY " + column + " ORDER BY total_payroll_usd DESC";
        return jdbcTemplate.query(
                sql,
                (rs, rowNum) -> new DimensionBreakdown(
                        rs.getString("dimension"), rs.getLong("headcount"), rs.getBigDecimal("total_payroll_usd")));
    }

    public List<DistributionBucket> distribution() {
        String sql = CURRENT_USD_SALARY_CTE
                + """
                SELECT
                    CASE
                        WHEN usd_salary < 50000 THEN '<50k'
                        WHEN usd_salary < 75000 THEN '50k-75k'
                        WHEN usd_salary < 100000 THEN '75k-100k'
                        WHEN usd_salary < 125000 THEN '100k-125k'
                        WHEN usd_salary < 150000 THEN '125k-150k'
                        WHEN usd_salary < 200000 THEN '150k-200k'
                        ELSE '200k+'
                    END AS bucket,
                    COUNT(*) AS headcount
                FROM current_usd_salary
                GROUP BY bucket
                ORDER BY MIN(usd_salary)
                """;
        return jdbcTemplate.query(
                sql, (rs, rowNum) -> new DistributionBucket(rs.getString("bucket"), rs.getLong("headcount")));
    }
}
