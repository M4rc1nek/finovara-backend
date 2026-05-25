package com.finovara.activityservice.contracts.event.expense;


import com.finovara.activityservice.contracts.model.activity.ExpenseActivityType;
import com.finovara.activityservice.contracts.model.transaction.ExpenseCategory;

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