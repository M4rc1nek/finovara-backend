package com.finovara.contracts.event.piggybank;

import com.finovara.contracts.model.activity.PiggyBankActivityType;
import com.finovara.contracts.model.transaction.PiggyBankGoalType;

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