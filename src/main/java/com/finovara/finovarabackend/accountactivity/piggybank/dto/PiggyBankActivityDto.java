package com.finovara.finovarabackend.accountactivity.piggybank.dto;

import com.finovara.finovarabackend.accountactivity.piggybank.model.PiggyBankActivityType;
import com.finovara.finovarabackend.piggybank.model.PiggyBankGoalType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PiggyBankActivityDto(
        String piggyBankName,
        String previousPiggyBankName,
        PiggyBankActivityType activityType,
        PiggyBankGoalType goalType,
        PiggyBankGoalType previousGoalType,
        BigDecimal goalAmount,
        BigDecimal previousGoalAmount,
        BigDecimal amountPaid,
        BigDecimal amountPaidOut,
        LocalDateTime createdAt
) {
}
