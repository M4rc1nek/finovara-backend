package com.finovara.activitylogservice.activitylog.mainaccountactivity.piggybank.dto;

import com.finovara.contracts.mainaccount.activity.model.PiggyBankActivityType;
import com.finovara.contracts.util.model.PiggyBankGoalType;

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
