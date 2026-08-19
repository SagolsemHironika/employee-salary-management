package com.acme.salary.employee;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.acme.salary.AbstractIntegrationTest;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;

class EmployeeControllerIT extends AbstractIntegrationTest {

    private static final String DEPARTMENT = "QA-Dept-EmployeeControllerIT";

    @Autowired
    private EmployeeRepository employeeRepository;

    @BeforeEach
    void seedFixtureEmployees() {
        save("ZZ-001", "ZZ", DEPARTMENT);
        save("ZZ-002", "ZZ", DEPARTMENT);
        save("YY-001", "YY", DEPARTMENT);
    }

    private void save(String code, String country, String department) {
        Employee employee = new Employee();
        employee.setEmployeeCode(code + "-" + System.nanoTime());
        employee.setFirstName("Test");
        employee.setLastName(code);
        employee.setEmail(code.toLowerCase() + "-" + System.nanoTime() + "@it.example");
        employee.setCountryCode(country);
        employee.setDepartment(department);
        employee.setJobTitle("Test Engineer");
        employee.setBand("L2");
        employee.setHireDate(LocalDate.of(2022, 1, 1));
        employee.setStatus("active");
        employeeRepository.save(employee);
    }

    @Test
    void listWithoutTokenReturns401() throws Exception {
        mockMvc.perform(get("/employees")).andExpect(status().isUnauthorized());
    }

    @Test
    void filtersByDepartmentAndCountry() throws Exception {
        String token = adminToken();

        mockMvc.perform(get("/employees")
                        .param("department", DEPARTMENT)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3));

        mockMvc.perform(get("/employees")
                        .param("department", DEPARTMENT)
                        .param("country", "ZZ")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void paginatesResults() throws Exception {
        String token = adminToken();

        mockMvc.perform(get("/employees")
                        .param("department", DEPARTMENT)
                        .param("page", "0")
                        .param("size", "1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.size").value(1));
    }
}
