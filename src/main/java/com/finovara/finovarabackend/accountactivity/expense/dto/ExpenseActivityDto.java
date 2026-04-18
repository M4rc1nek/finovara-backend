package com.finovara.finovarabackend.accountactivity.expense.dto;

import com.finovara.finovarabackend.accountactivity.expense.model.ExpenseActivityType;
import com.finovara.finovarabackend.expense.model.ExpenseCategory;

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
