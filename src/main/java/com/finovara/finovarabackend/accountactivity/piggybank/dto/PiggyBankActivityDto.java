package com.finovara.finovarabackend.accountactivity.piggybank.dto;

import com.finovara.finovarabackend.accountactivity.piggybank.model.PiggyBankActivityType;
import com.finovara.finovarabackend.piggybank.model.PiggyBankGoalType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PiggyBankActivityDto(
        String piggyBankName,
        PiggyBankActivityType activityType,
        PiggyBankGoalType goalType,
        BigDecimal goalAmount,
        BigDecimal amountPaid,
        BigDecimal amountPaidOut,
        LocalDateTime date
) {
}
