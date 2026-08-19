package com.finovara.contracts.activity.event.sharedaccount;

import com.finovara.contracts.model.activity.SharedAccountActivityType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SharedAccountActivityEvent(
        Long userId,
        SharedAccountActivityType type,
        BigDecimal refundedBalance,
        String coFounderUsername,
        String coFounderEmail,
        LocalDateTime occurredAt
) {
}
