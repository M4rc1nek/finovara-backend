package com.finovara.contracts.finance.event.sharedaccount;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record GoalAchievedNotificationEvent(
        Long ownerId,
        Long memberId,
        Long triggeredByUserId,
        Long piggyBankId,
        BigDecimal currentAmount,
        BigDecimal goalAmount,
        LocalDateTime occurredAt
) {
}