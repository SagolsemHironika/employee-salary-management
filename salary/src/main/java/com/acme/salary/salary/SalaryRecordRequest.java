package com.acme.salary.salary;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record SalaryRecordRequest(
        @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal baseSalary,
        @NotBlank @Size(min = 3, max = 3) String currencyCode,
        @DecimalMin(value = "0.0") BigDecimal bonus,
        @DecimalMin(value = "0.0") BigDecimal allowances,
        @NotNull LocalDate effectiveDate,
        @NotBlank @Pattern(regexp = "hire|promotion|annual_review|adjustment") String changeReason) {

    public BigDecimal bonusOrZero() {
        return bonus == null ? BigDecimal.ZERO : bonus;
    }

    public BigDecimal allowancesOrZero() {
        return allowances == null ? BigDecimal.ZERO : allowances;
    }
}
