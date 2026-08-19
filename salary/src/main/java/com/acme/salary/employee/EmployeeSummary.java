package com.acme.salary.employee;

import java.time.LocalDate;

public record EmployeeSummary(
        Long id,
        String employeeCode,
        String firstName,
        String lastName,
        String email,
        String countryCode,
        String department,
        String jobTitle,
        String band,
        LocalDate hireDate,
        String status) {

    static EmployeeSummary from(Employee employee) {
        return new EmployeeSummary(
                employee.getId(),
                employee.getEmployeeCode(),
                employee.getFirstName(),
                employee.getLastName(),
                employee.getEmail(),
                employee.getCountryCode(),
                employee.getDepartment(),
                employee.getJobTitle(),
                employee.getBand(),
                employee.getHireDate(),
                employee.getStatus());
    }
}
