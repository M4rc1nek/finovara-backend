package com.finovara.activitylogservice.activitylog.mainaccount.sharedaccount.dto;

import com.finovara.contracts.mainaccount.activity.model.SharedAccountChangeHistoryActivityType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SharedAccountChangeHistoryActivityDto(
        SharedAccountChangeHistoryActivityType type,
        BigDecimal refundedBalance,
        String coFounderUsername,
        String coFounderEmail,
        LocalDateTime createdAt
) {
}
