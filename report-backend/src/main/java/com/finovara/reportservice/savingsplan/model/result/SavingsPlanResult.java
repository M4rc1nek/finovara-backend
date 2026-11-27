package com.finovara.reportservice.savingsplan.model.result;

import com.finovara.reportservice.savingsplan.dto.response.model.GoalResult;
import com.finovara.reportservice.savingsplan.dto.response.model.PlanOption;
import com.finovara.reportservice.savingsplan.model.amount.SavingsPlanAmounts;

import java.math.BigDecimal;
import java.util.List;

public record SavingsPlanResult(
        SavingsPlanAmounts amounts,
        BigDecimal moneySavedPerMonth,
        SavingsPlanDecision decision
) {

    public int monthsToTarget() {
        return amounts.monthsToTarget();
    }

    public BigDecimal moneyNeededPerMonth() {
        return amounts.moneyNeededPerMonth();
    }

    public BigDecimal moneyAvailablePerMonth() {
        return amounts.moneyAvailablePerMonth();
    }

    public GoalResult result() {
        return decision.result();
    }

    public List<PlanOption> options() {
        return decision.options();
    }

    public boolean highPaymentWarning() {
        return decision.highPaymentWarning();
    }

    public String message() {
        return decision.message();
    }
}
