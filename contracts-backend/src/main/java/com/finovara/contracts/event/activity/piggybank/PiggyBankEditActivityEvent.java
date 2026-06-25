package com.finovara.contracts.event.activity.piggybank;

import com.finovara.contracts.model.activity.PiggyBankActivityType;
import com.finovara.contracts.model.transaction.PiggyBankGoalType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PiggyBankEditActivityEvent(
        Long userId,
        PiggyBankActivityType type,
        String name,
        String previousName,
        PiggyBankGoalType goalType,
        PiggyBankGoalType previousGoalType,
        BigDecimal goalAmount,
        BigDecimal previousGoalAmount,
        LocalDateTime occurredAt
) {
}