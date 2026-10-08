package com.finovara.activitylogservice.activitylog.mainaccountactivity.expense.dto;

import com.finovara.contracts.mainaccount.activity.model.ExpenseActivityType;
import com.finovara.contracts.util.model.ExpenseCategory;

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
