package com.finovara.activityservice.activity_log.accountactivity.piggybank.dto;

import com.finovara.contracts.model.activity.PiggyBankActivityType;
import com.finovara.contracts.model.transaction.PiggyBankGoalType;

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
