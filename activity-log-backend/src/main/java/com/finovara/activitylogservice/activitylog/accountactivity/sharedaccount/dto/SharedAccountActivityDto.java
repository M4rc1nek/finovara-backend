package com.finovara.activitylogservice.activitylog.accountactivity.sharedaccount.dto;

import com.finovara.contracts.model.activity.SharedAccountActivityType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SharedAccountActivityDto(
        SharedAccountActivityType type,
        BigDecimal refundedBalance,
        String coFounderUsername,
        String coFounderEmail,
        LocalDateTime createdAt
) {
}
