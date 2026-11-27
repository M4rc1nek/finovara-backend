package com.finovara.reportservice.savingsplan.model.amount;

import java.math.BigDecimal;

public record SavingsPlanAmounts(
        BigDecimal amountStillNeededForGoal,
        int monthsToTarget,
        BigDecimal moneyNeededPerMonth,
        BigDecimal moneyAvailablePerMonth
) {
}
