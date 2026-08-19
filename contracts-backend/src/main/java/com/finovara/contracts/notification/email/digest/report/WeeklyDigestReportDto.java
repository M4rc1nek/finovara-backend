package com.finovara.contracts.notification.email.digest.report;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DigestReportDto(
        Long userId,
        LocalDate weekStart,
        LocalDate weekEnd,
        BigDecimal expensesSum,
        String topExpenseCategory,
        BigDecimal revenuesSum,
        String topRevenueCategory,
        BigDecimal remainingBudgetPercentage,
        BigDecimal savedMoney,
        Integer daysWithoutExpense,
        BigDecimal highestExpenseAmount,
        String highestExpenseCategory,
        LocalDate highestExpenseDate
) {
}