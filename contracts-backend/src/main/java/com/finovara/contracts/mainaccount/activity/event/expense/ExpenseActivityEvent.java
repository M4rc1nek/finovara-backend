package com.finovara.contracts.mainaccount.activity.event.expense;


import com.finovara.contracts.mainaccount.activity.model.ExpenseActivityType;
import com.finovara.contracts.util.model.ExpenseCategory;

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