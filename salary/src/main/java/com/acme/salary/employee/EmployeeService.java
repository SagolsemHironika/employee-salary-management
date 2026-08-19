package com.acme.salary.employee;

import com.acme.salary.common.PagedResponse;
import com.acme.salary.common.ResourceNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class EmployeeService {

    private final EmployeeRepository employeeRepository;

    public EmployeeService(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    public PagedResponse<EmployeeSummary> list(String countryCode, String department, Pageable pageable) {
        Page<Employee> page = find(countryCode, department, pageable);
        return PagedResponse.of(page.map(EmployeeSummary::from));
    }

    public EmployeeSummary get(Long id) {
        return employeeRepository
                .findById(id)
                .map(EmployeeSummary::from)
                .orElseThrow(() -> new ResourceNotFoundException("Employee " + id + " not found"));
    }

    private Page<Employee> find(String countryCode, String department, Pageable pageable) {
        boolean hasCountry = StringUtils.hasText(countryCode);
        boolean hasDepartment = StringUtils.hasText(department);

        if (hasCountry && hasDepartment) {
            return employeeRepository.findByCountryCodeAndDepartment(countryCode, department, pageable);
        }
        if (hasCountry) {
            return employeeRepository.findByCountryCode(countryCode, pageable);
        }
        if (hasDepartment) {
            return employeeRepository.findByDepartment(department, pageable);
        }
        return employeeRepository.findAll(pageable);
    }
}
