package com.finovara.contracts.mainaccount.activity.event.sharedaccount;

import com.finovara.contracts.mainaccount.activity.model.SharedAccountActivityType;

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
