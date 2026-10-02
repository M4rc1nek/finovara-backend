package com.finovara.contracts.sharedaccount.event.activity.finance;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SharedAccountExpenseActivityEvent(
        Long ownerId,
        Long memberId,
        Long userId,
        Long expenseId,
        BigDecimal amount,
        String category,
        LocalDateTime createdAt
) {
}