package com.finovara.activitylogservice.activitylog.mainaccountactivity.sharedaccount.dto;

import com.finovara.contracts.mainaccount.activity.model.SharedAccountActivityType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SharedAccountChangeHistoryActivityDto(
        SharedAccountActivityType type,
        BigDecimal refundedBalance,
        String coFounderUsername,
        String coFounderEmail,
        LocalDateTime createdAt
) {
}
