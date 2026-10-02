package com.finovara.contracts.finance.event.sharedaccount;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record LargeExpenseNotificationEvent(
        Long ownerId,
        Long memberId,
        Long triggeredByUserId,
        Long expenseId,
        BigDecimal amount,
        BigDecimal threshold,
        LocalDateTime occurredAt
) {}