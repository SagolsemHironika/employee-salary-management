package com.acme.salary.employee;

import com.acme.salary.salary.SalaryRecord;
import java.math.BigDecimal;
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
        String status,
        BigDecimal currentBaseSalary,
        String currentCurrencyCode) {

    static EmployeeSummary from(Employee employee, SalaryRecord currentSalary) {
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
                employee.getStatus(),
                currentSalary == null ? null : currentSalary.getBaseSalary(),
                currentSalary == null ? null : currentSalary.getCurrencyCode());
    }
}
