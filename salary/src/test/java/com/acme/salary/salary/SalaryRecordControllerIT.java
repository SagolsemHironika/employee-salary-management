package com.acme.salary.salary;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.acme.salary.AbstractIntegrationTest;
import com.acme.salary.employee.Employee;
import com.acme.salary.employee.EmployeeRepository;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;

class SalaryRecordControllerIT extends AbstractIntegrationTest {

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Long employeeId;

    @BeforeEach
    void seedEmployee() {
        Employee employee = new Employee();
        employee.setEmployeeCode("SR-IT-" + System.nanoTime());
        employee.setFirstName("Salary");
        employee.setLastName("History");
        employee.setEmail("salary.history." + System.nanoTime() + "@it.example");
        employee.setCountryCode("US");
        employee.setDepartment("Engineering");
        employee.setJobTitle("Test Engineer");
        employee.setBand("L2");
        employee.setHireDate(LocalDate.of(2021, 3, 15));
        employee.setStatus("active");
        employeeId = employeeRepository.save(employee).getId();
    }

    @Test
    void databaseRejectsASecondOpenSalaryRecordForOneEmployee() throws Exception {
        String token = adminToken();
        mockMvc.perform(post("/employees/" + employeeId + "/salary-records")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(hireRecordJson("2021-03-15")))
                .andExpect(status().isCreated());

        // Deliberately bypasses the service to insert straight into the table.
        // The point is that "one current salary per employee" holds because the
        // schema enforces it, not because every caller remembers to -- so it
        // survives bulk imports, migrations and manual SQL too. Asserting it
        // through the API would only re-test the service's own bookkeeping.
        assertThatThrownBy(() -> jdbcTemplate.update(
                        """
                        INSERT INTO salary_records
                            (employee_id, base_salary, currency_code, effective_date, change_reason, created_by)
                        VALUES (?, 120000, 'USD', DATE '2024-01-01', 'adjustment', 'rogue-writer')
                        """,
                        employeeId))
                .isInstanceOf(DuplicateKeyException.class)
                .hasMessageContaining("uq_salary_records_open_per_employee");
    }

    @Test
    void createWithoutTokenReturns401() throws Exception {
        mockMvc.perform(post("/employees/" + employeeId + "/salary-records")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(hireRecordJson("2021-03-15")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createsHireRecordAttributedToAuthenticatedUser() throws Exception {
        String token = adminToken();

        mockMvc.perform(post("/employees/" + employeeId + "/salary-records")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(hireRecordJson("2021-03-15")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.employeeId").value(employeeId))
                .andExpect(jsonPath("$.endDate").doesNotExist())
                .andExpect(jsonPath("$.createdBy").value("admin@acme.example"));
    }

    @Test
    void secondRecordClosesThePreviousOne() throws Exception {
        String token = adminToken();

        mockMvc.perform(post("/employees/" + employeeId + "/salary-records")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(hireRecordJson("2021-03-15")));

        mockMvc.perform(post("/employees/" + employeeId + "/salary-records")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {"baseSalary":105000,"currencyCode":"USD","effectiveDate":"2023-01-01","changeReason":"promotion"}
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/employees/" + employeeId + "/salary-records")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].effectiveDate").value("2023-01-01"))
                .andExpect(jsonPath("$[0].endDate").doesNotExist())
                .andExpect(jsonPath("$[1].effectiveDate").value("2021-03-15"))
                .andExpect(jsonPath("$[1].endDate").value("2022-12-31"));
    }

    @Test
    void rejectsOutOfOrderEffectiveDate() throws Exception {
        String token = adminToken();

        mockMvc.perform(post("/employees/" + employeeId + "/salary-records")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(hireRecordJson("2023-01-01")));

        mockMvc.perform(post("/employees/" + employeeId + "/salary-records")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(hireRecordJson("2022-01-01")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void historyForUnknownEmployeeReturns404() throws Exception {
        String token = adminToken();

        mockMvc.perform(get("/employees/999999999/salary-records").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    private static String hireRecordJson(String effectiveDate) {
        return """
                {"baseSalary":90000,"currencyCode":"USD","effectiveDate":"%s","changeReason":"hire"}
                """
                .formatted(effectiveDate);
    }
}
