package com.finovara.contracts.mainaccount.transaction.event.piggybank;

import com.finovara.contracts.util.model.PiggyBankGoalType;

import java.math.BigDecimal;

public record PiggyBankProgressEvent(
        Long userId,
        Long piggyBankId,
        BigDecimal percentage,
        PiggyBankGoalType goalType,
        String piggyBankName
) {
}
