package com.finovara.activitylogservice.activitylog.accountactivity.expense.dto;

import com.finovara.contracts.model.activity.ExpenseActivityType;
import com.finovara.contracts.model.transaction.ExpenseCategory;

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
