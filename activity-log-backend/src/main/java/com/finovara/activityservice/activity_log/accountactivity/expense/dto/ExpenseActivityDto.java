package com.finovara.activityservice.activity_log.accountactivity.expense.dto;

import com.finovara.activityservice.contracts.model.activity.ExpenseActivityType;
import com.finovara.activityservice.contracts.model.transaction.ExpenseCategory;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ExpenseActivityDto(
        ExpenseActivityType type,
        BigDecimal amount,
        BigDecimal previousAmount,
        ExpenseCategory category,
        ExpenseCategory previousCategory,
        LocalDateTime createdAt
) {
}
