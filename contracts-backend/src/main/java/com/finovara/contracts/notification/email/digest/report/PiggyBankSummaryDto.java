package com.finovara.financeservice.internal.digest.report.email.dto;

import java.math.BigDecimal;

public record PiggyBankSummaryDto(
        long quantityOfPiggyBanks,
        BigDecimal totalDepositedMoney,
        BigDecimal progressPercentage,
        BigDecimal remainingAmount,
        boolean goalCompleted
) {
}