package com.finovara.contracts.mainaccount.activity.event.sharedaccount;

import com.finovara.contracts.mainaccount.activity.model.SharedAccountChangeHistoryActivityType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SharedAccountChangeHistoryActivityEvent(
        Long userId,
        SharedAccountChangeHistoryActivityType type,
        BigDecimal refundedBalance,
        String coFounderUsername,
        String coFounderEmail,
        LocalDateTime occurredAt
) {
}
