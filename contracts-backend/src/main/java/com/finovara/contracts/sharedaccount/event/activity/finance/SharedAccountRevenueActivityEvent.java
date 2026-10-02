package com.finovara.contracts.sharedaccount.event.activity.finance;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SharedAccountRevenueActivityEvent(
        Long ownerId,
        Long memberId,
        Long userId,
        Long revenueId,
        BigDecimal amount,
        String category,
        LocalDateTime createdAt) {}
