package com.finovara.contracts.mainaccount.report.digest.finance;

import java.math.BigDecimal;

public record PiggyBankSummaryDto(
        long quantityOfPiggyBanks,
        BigDecimal totalDepositedMoney,
        BigDecimal progressPercentage,
        BigDecimal remainingAmount,
        boolean goalCompleted
) {
}