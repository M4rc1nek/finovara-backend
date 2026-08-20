package com.finovara.contracts.notification.email.digest.report.finance;

import java.math.BigDecimal;

public record PiggyBankSummaryDto(
        long quantityOfPiggyBanks,
        BigDecimal totalDepositedMoney,
        BigDecimal progressPercentage,
        BigDecimal remainingAmount,
        boolean goalCompleted
) {
}