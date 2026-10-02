package com.finovara.contracts.mainaccount.activity.event.piggybank;

import com.finovara.contracts.mainaccount.activity.model.PiggyBankActivityType;
import com.finovara.contracts.util.model.PiggyBankGoalType;

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