package com.finovara.contracts.event.activity.expense;


import com.finovara.contracts.model.activity.ExpenseActivityType;
import com.finovara.contracts.model.transaction.ExpenseCategory;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ExpenseActivityEvent(
        Long userId,
        ExpenseActivityType type,
        BigDecimal amount,
        ExpenseCategory category,
        BigDecimal previousAmount,
        ExpenseCategory previousCategory,
        LocalDateTime occurredAt
) {}