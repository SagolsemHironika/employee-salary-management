package com.acme.salary.employee;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    Page<Employee> findByCountryCodeAndDepartment(String countryCode, String department, Pageable pageable);

    Page<Employee> findByCountryCode(String countryCode, Pageable pageable);

    Page<Employee> findByDepartment(String department, Pageable pageable);
}
