package com.acme.salary.salary;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SalaryRecordSummary(
        Long id,
        Long employeeId,
        BigDecimal baseSalary,
        String currencyCode,
        BigDecimal bonus,
        BigDecimal allowances,
        LocalDate effectiveDate,
        LocalDate endDate,
        String changeReason,
        String createdBy) {

    static SalaryRecordSummary from(SalaryRecord record) {
        return new SalaryRecordSummary(
                record.getId(),
                record.getEmployeeId(),
                record.getBaseSalary(),
                record.getCurrencyCode(),
                record.getBonus(),
                record.getAllowances(),
                record.getEffectiveDate(),
                record.getEndDate(),
                record.getChangeReason(),
                record.getCreatedBy());
    }
}
