package com.finovara.activityservice.contracts.event.piggybank;

import com.finovara.activityservice.contracts.model.activity.PiggyBankActivityType;
import com.finovara.activityservice.contracts.model.transaction.PiggyBankGoalType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PiggyBankActivityEvent(
        Long userId,
        PiggyBankActivityType type,
        String name,
        PiggyBankGoalType goalType,
        BigDecimal goalAmount,
        BigDecimal amountPaid,
        LocalDateTime occurredAt
) {}