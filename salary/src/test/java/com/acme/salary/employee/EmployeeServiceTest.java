package com.acme.salary.employee;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.acme.salary.common.ResourceNotFoundException;
import com.acme.salary.salary.SalaryRecord;
import com.acme.salary.salary.SalaryRecordRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * The list() method's filtering/sorting/pagination is exercised end-to-end
 * against a real Postgres in EmployeeControllerIT - it builds a raw SQL
 * query (see EmployeeService.FROM_AND_FILTER), so a real database is what
 * actually proves it correct rather than mocking JdbcTemplate's string
 * arguments. Unit coverage here is for get(), which stays JPA-based.
 */
@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private SalaryRecordRepository salaryRecordRepository;

    @Mock
    private JdbcTemplate jdbcTemplate;

    private EmployeeService service;

    @BeforeEach
    void setUp() {
        service = new EmployeeService(employeeRepository, salaryRecordRepository, jdbcTemplate);
    }

    @Test
    void getReturnsSummaryWithCurrentSalaryWhenEmployeeExists() {
        Employee employee = new Employee();
        employee.setId(5L);
        employee.setEmployeeCode("EMP-00005");
        employee.setFirstName("Ada");
        employee.setLastName("Lovelace");
        employee.setEmail("ada@acme.example");
        employee.setCountryCode("GB");
        employee.setDepartment("Engineering");
        employee.setJobTitle("Software Engineer");
        employee.setBand("ENG-L3");
        employee.setHireDate(LocalDate.of(2020, 1, 1));
        employee.setStatus("active");
        when(employeeRepository.findById(5L)).thenReturn(Optional.of(employee));

        SalaryRecord currentSalary = new SalaryRecord();
        currentSalary.setBaseSalary(new BigDecimal("95000.00"));
        currentSalary.setCurrencyCode("USD");
        when(salaryRecordRepository.findByEmployeeIdAndEndDateIsNull(5L)).thenReturn(Optional.of(currentSalary));

        EmployeeSummary result = service.get(5L);

        assertThat(result.id()).isEqualTo(5L);
        assertThat(result.email()).isEqualTo("ada@acme.example");
        assertThat(result.currentBaseSalary()).isEqualByComparingTo("95000.00");
        assertThat(result.currentCurrencyCode()).isEqualTo("USD");
    }

    @Test
    void getReturnsNullSalaryWhenEmployeeHasNoCurrentRecord() {
        Employee employee = new Employee();
        employee.setId(6L);
        employee.setEmployeeCode("EMP-00006");
        employee.setFirstName("Grace");
        employee.setLastName("Hopper");
        employee.setEmail("grace@acme.example");
        employee.setCountryCode("US");
        employee.setDepartment("Engineering");
        employee.setJobTitle("Software Engineer");
        employee.setBand("ENG-L3");
        employee.setHireDate(LocalDate.of(2020, 1, 1));
        employee.setStatus("active");
        when(employeeRepository.findById(6L)).thenReturn(Optional.of(employee));
        when(salaryRecordRepository.findByEmployeeIdAndEndDateIsNull(6L)).thenReturn(Optional.empty());

        EmployeeSummary result = service.get(6L);

        assertThat(result.currentBaseSalary()).isNull();
        assertThat(result.currentCurrencyCode()).isNull();
    }

    @Test
    void getThrowsWhenEmployeeDoesNotExist() {
        when(employeeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(99L)).isInstanceOf(ResourceNotFoundException.class);
    }
}
