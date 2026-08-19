package com.finovara.financeservice.internal.digest.report.email.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RevenueSummary(
        BigDecimal sum,
        String topCategory,
        BigDecimal highestAmount,
        String highestCategory,
        LocalDate highestDate
) {
}