package com.finovara.contracts.finance.event.sharedaccount.finance;

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
