package com.finovara.contracts.notification.event.piggybank;

import com.finovara.contracts.model.transaction.PiggyBankGoalType;

import java.math.BigDecimal;

public record PiggyBankProgressEvent(
        Long userId,
        Long piggyBankId,
        BigDecimal percentage,
        PiggyBankGoalType goalType,
        String piggyBankName
) {
}
