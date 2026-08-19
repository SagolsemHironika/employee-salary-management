package com.acme.salary.analytics;

import java.math.BigDecimal;

public record DimensionBreakdown(String dimension, long headcount, BigDecimal totalPayrollUsd) {}
