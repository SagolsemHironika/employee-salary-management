package com.acme.salary.employee;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.acme.salary.AbstractIntegrationTest;
import com.acme.salary.salary.SalaryRecord;
import com.acme.salary.salary.SalaryRecordRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;

class EmployeeControllerIT extends AbstractIntegrationTest {

    private static final String DEPARTMENT = "QA-Dept-EmployeeControllerIT";
    private static final String SORT_DEPARTMENT = "Sort-Dept-EmployeeControllerIT";

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private SalaryRecordRepository salaryRecordRepository;

    @BeforeEach
    void seedFixtureEmployees() {
        save("ZZ-001", "ZZ", DEPARTMENT);
        save("ZZ-002", "ZZ", DEPARTMENT);
        save("YY-001", "YY", DEPARTMENT);
    }

    private Employee save(String code, String country, String department) {
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
        return employeeRepository.save(employee);
    }

    private void giveSalary(Employee employee, String amount, String currencyCode) {
        SalaryRecord record = new SalaryRecord();
        record.setEmployeeId(employee.getId());
        record.setCurrencyCode(currencyCode);
        record.setBaseSalary(new BigDecimal(amount));
        record.setBonus(BigDecimal.ZERO);
        record.setAllowances(BigDecimal.ZERO);
        record.setEffectiveDate(LocalDate.of(2022, 1, 1));
        record.setChangeReason("hire");
        record.setCreatedBy("it-test");
        salaryRecordRepository.save(record);
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

    @Test
    void defaultOrderIsByNameRatherThanIdOrEmployeeCode() throws Exception {
        String token = adminToken();
        // Inserted (and so id/employee_code-ordered) as Zeta, Alpha, Mike;
        // name order is Alpha, Mike, Zeta. If the default order were ever
        // id-based, this would come back Zeta, Alpha, Mike instead - and
        // separately, an id-based default is indistinguishable from an
        // explicit "employeeCode ascending" sort, which is exactly the bug
        // this default was changed to avoid (see EmployeeService).
        save("Zeta", "US", SORT_DEPARTMENT);
        save("Alpha", "US", SORT_DEPARTMENT);
        save("Mike", "US", SORT_DEPARTMENT);

        mockMvc.perform(get("/employees")
                        .param("department", SORT_DEPARTMENT)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].lastName").value("Alpha"))
                .andExpect(jsonPath("$.items[1].lastName").value("Mike"))
                .andExpect(jsonPath("$.items[2].lastName").value("Zeta"));
    }

    @Test
    void sortsByEmployeeIdInBothDirections() throws Exception {
        String token = adminToken();
        Employee a = save("A", "US", SORT_DEPARTMENT);
        Employee b = save("B", "US", SORT_DEPARTMENT);
        Employee c = save("C", "US", SORT_DEPARTMENT);
        List<String> ascending =
                Stream.of(a, b, c).map(Employee::getEmployeeCode).sorted().toList();

        mockMvc.perform(get("/employees")
                        .param("department", SORT_DEPARTMENT)
                        .param("sort", "employeeCode,asc")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].employeeCode").value(ascending.get(0)))
                .andExpect(jsonPath("$.items[1].employeeCode").value(ascending.get(1)))
                .andExpect(jsonPath("$.items[2].employeeCode").value(ascending.get(2)));

        mockMvc.perform(get("/employees")
                        .param("department", SORT_DEPARTMENT)
                        .param("sort", "employeeCode,desc")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].employeeCode").value(ascending.get(2)))
                .andExpect(jsonPath("$.items[2].employeeCode").value(ascending.get(0)));
    }

    @Test
    void sortsBySalaryDescendingNormalizedToUsdAcrossCurrencies() throws Exception {
        String token = adminToken();
        Employee midUsd = save("MID", "US", SORT_DEPARTMENT);
        Employee bigNominalButSmallUsd = save("BIGNOMINAL", "NG", SORT_DEPARTMENT);
        Employee topUsd = save("TOP", "US", SORT_DEPARTMENT);

        giveSalary(midUsd, "50000", "USD"); // 50,000 USD
        giveSalary(bigNominalButSmallUsd, "50000000", "NGN"); // 50,000,000 NGN * 0.00062 = 31,000 USD equivalent
        giveSalary(topUsd, "200000", "USD"); // 200,000 USD

        // A raw, currency-blind sort would put bigNominalButSmallUsd first (50,000,000 > 200,000
        // as a bare number); the correct USD-normalized order is topUsd, midUsd, bigNominalButSmallUsd.
        mockMvc.perform(get("/employees")
                        .param("department", SORT_DEPARTMENT)
                        .param("sort", "salary,desc")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].employeeCode").value(topUsd.getEmployeeCode()))
                .andExpect(jsonPath("$.items[1].employeeCode").value(midUsd.getEmployeeCode()))
                .andExpect(jsonPath("$.items[2].employeeCode").value(bigNominalButSmallUsd.getEmployeeCode()));
    }

    @Test
    void unknownSortPropertyFallsBackSafelyInsteadOfErroring() throws Exception {
        String token = adminToken();

        mockMvc.perform(get("/employees")
                        .param("department", DEPARTMENT)
                        .param("sort", "'; DROP TABLE employees; --,asc")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3));
    }
}
