package com.acme.salary.analytics;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.acme.salary.AbstractIntegrationTest;
import com.acme.salary.employee.Employee;
import com.acme.salary.employee.EmployeeRepository;
import com.acme.salary.salary.SalaryRecord;
import com.acme.salary.salary.SalaryRecordRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MvcResult;

class AnalyticsControllerIT extends AbstractIntegrationTest {

    private static final String DEPARTMENT = "Analytics-IT-Dept";

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private SalaryRecordRepository salaryRecordRepository;

    @BeforeEach
    void seedFixtureWithKnownUsdSalaries() {
        // Both employees are USD (rate_to_usd = 1.0), so the expected totals
        // are exact: headcount 2, total payroll 300000.00.
        seedEmployeeWithSalary("100000");
        seedEmployeeWithSalary("200000");
    }

    private void seedEmployeeWithSalary(String usdBaseSalary) {
        Employee employee = new Employee();
        employee.setEmployeeCode("AN-IT-" + System.nanoTime());
        employee.setFirstName("Analytics");
        employee.setLastName("Fixture");
        employee.setEmail("analytics.fixture." + System.nanoTime() + "@it.example");
        employee.setCountryCode("US");
        employee.setDepartment(DEPARTMENT);
        employee.setJobTitle("Test Engineer");
        employee.setBand("L3");
        employee.setHireDate(LocalDate.of(2021, 1, 1));
        employee.setStatus("active");
        Employee saved = employeeRepository.save(employee);

        SalaryRecord record = new SalaryRecord();
        record.setEmployeeId(saved.getId());
        record.setCurrencyCode("USD");
        record.setBaseSalary(new BigDecimal(usdBaseSalary));
        record.setBonus(BigDecimal.ZERO);
        record.setAllowances(BigDecimal.ZERO);
        record.setEffectiveDate(LocalDate.of(2021, 1, 1));
        record.setChangeReason("hire");
        record.setCreatedBy("it-test");
        salaryRecordRepository.save(record);
    }

    @Test
    void byDimensionReturnsExactHeadcountAndPayrollForFixtureDepartment() throws Exception {
        String token = adminToken();

        MvcResult result = mockMvc.perform(get("/analytics/by-dimension")
                        .param("groupBy", "department")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        var breakdowns = objectMapper.readValue(
                result.getResponse().getContentAsString(), DimensionBreakdown[].class);

        DimensionBreakdown fixtureRow = java.util.Arrays.stream(breakdowns)
                .filter(row -> DEPARTMENT.equals(row.dimension()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Fixture department not found in response"));

        org.assertj.core.api.Assertions.assertThat(fixtureRow.headcount()).isEqualTo(2);
        org.assertj.core.api.Assertions.assertThat(fixtureRow.totalPayrollUsd())
                .isEqualByComparingTo("300000.00");
    }

    @Test
    void byDimensionRejectsUnknownGroupBy() throws Exception {
        String token = adminToken();

        mockMvc.perform(get("/analytics/by-dimension")
                        .param("groupBy", "nonsense")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isBadRequest());
    }

    @Test
    void summaryWithoutTokenReturns401() throws Exception {
        mockMvc.perform(get("/analytics/summary")).andExpect(status().isUnauthorized());
    }
}
