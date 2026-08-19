package com.acme.salary.salary;

import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SalaryRecordController {

    private final SalaryRecordService salaryRecordService;

    public SalaryRecordController(SalaryRecordService salaryRecordService) {
        this.salaryRecordService = salaryRecordService;
    }

    @PostMapping("/employees/{employeeId}/salary-records")
    @ResponseStatus(HttpStatus.CREATED)
    public SalaryRecordSummary create(
            @PathVariable Long employeeId, @Valid @RequestBody SalaryRecordRequest request, Principal principal) {
        return salaryRecordService.createRecord(employeeId, request, principal.getName());
    }

    @GetMapping("/employees/{employeeId}/salary-records")
    public List<SalaryRecordSummary> history(@PathVariable Long employeeId) {
        return salaryRecordService.history(employeeId);
    }
}
