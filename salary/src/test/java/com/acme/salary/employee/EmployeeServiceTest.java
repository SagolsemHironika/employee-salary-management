package com.acme.salary.employee;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.acme.salary.common.PagedResponse;
import com.acme.salary.common.ResourceNotFoundException;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

    @Mock
    private EmployeeRepository employeeRepository;

    private EmployeeService service;
    private final Pageable pageable = PageRequest.of(0, 20);

    @BeforeEach
    void setUp() {
        service = new EmployeeService(employeeRepository);
    }

    @Test
    void queriesByCountryAndDepartmentWhenBothProvided() {
        Page<Employee> emptyPage = new PageImpl<>(java.util.List.of());
        when(employeeRepository.findByCountryCodeAndDepartment("US", "Engineering", pageable))
                .thenReturn(emptyPage);

        service.list("US", "Engineering", pageable);

        verify(employeeRepository).findByCountryCodeAndDepartment("US", "Engineering", pageable);
        verify(employeeRepository, never()).findByCountryCode(any(), any());
        verify(employeeRepository, never()).findByDepartment(any(), any());
        verify(employeeRepository, never()).findAll(any(Pageable.class));
    }

    @Test
    void queriesByCountryOnlyWhenDepartmentMissing() {
        when(employeeRepository.findByCountryCode("US", pageable)).thenReturn(new PageImpl<>(java.util.List.of()));

        service.list("US", null, pageable);

        verify(employeeRepository).findByCountryCode("US", pageable);
        verify(employeeRepository, never()).findByCountryCodeAndDepartment(any(), any(), any());
    }

    @Test
    void queriesByDepartmentOnlyWhenCountryMissing() {
        when(employeeRepository.findByDepartment("Engineering", pageable))
                .thenReturn(new PageImpl<>(java.util.List.of()));

        service.list(null, "Engineering", pageable);

        verify(employeeRepository).findByDepartment("Engineering", pageable);
    }

    @Test
    void queriesFindAllWhenNoFiltersProvided() {
        when(employeeRepository.findAll(pageable)).thenReturn(new PageImpl<>(java.util.List.of()));

        PagedResponse<EmployeeSummary> result = service.list(null, null, pageable);

        verify(employeeRepository).findAll(pageable);
        org.assertj.core.api.Assertions.assertThat(result.items()).isEmpty();
    }

    @Test
    void treatsBlankFiltersAsAbsent() {
        when(employeeRepository.findAll(pageable)).thenReturn(new PageImpl<>(java.util.List.of()));

        service.list("  ", "", pageable);

        verify(employeeRepository).findAll(pageable);
    }

    @Test
    void getReturnsSummaryWhenEmployeeExists() {
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

        EmployeeSummary result = service.get(5L);

        org.assertj.core.api.Assertions.assertThat(result.id()).isEqualTo(5L);
        org.assertj.core.api.Assertions.assertThat(result.email()).isEqualTo("ada@acme.example");
    }

    @Test
    void getThrowsWhenEmployeeDoesNotExist() {
        when(employeeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(99L)).isInstanceOf(ResourceNotFoundException.class);
    }
}
