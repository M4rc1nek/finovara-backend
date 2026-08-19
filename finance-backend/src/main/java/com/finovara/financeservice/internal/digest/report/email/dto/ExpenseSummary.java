package com.finovara.financeservice.internal.digest.report.email.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ExpenseSummary(
        BigDecimal sum,
        String topCategory,
        BigDecimal highestAmount,
        String highestCategory,
        LocalDate highestDate,
        int daysWithoutExpense,
        BigDecimal remainingBudgetPercentage
) {
}