package com.finovara.contracts.mainaccount.activity.event.piggybank;

import com.finovara.contracts.mainaccount.activity.model.PiggyBankActivityType;
import com.finovara.contracts.util.model.PiggyBankGoalType;

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