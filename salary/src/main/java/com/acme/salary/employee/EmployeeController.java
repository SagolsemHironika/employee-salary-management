package com.acme.salary.employee;

import com.acme.salary.common.PagedResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class EmployeeController {

    private final EmployeeService employeeService;

    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    @GetMapping("/employees")
    public PagedResponse<EmployeeSummary> list(
            @RequestParam(required = false) String country,
            @RequestParam(required = false) String department,
            Pageable pageable) {
        return employeeService.list(country, department, pageable);
    }

    @GetMapping("/employees/{id}")
    public EmployeeSummary get(@PathVariable Long id) {
        return employeeService.get(id);
    }
}
