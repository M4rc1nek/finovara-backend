package com.finovara.reportservice.savingsplan.model.input;

import com.finovara.reportservice.savingsplan.dto.request.SavingsPlanRequest;
import com.finovara.reportservice.savingsplan.dto.response.model.GoalResult;
import com.finovara.reportservice.savingsplan.dto.response.model.PlanOption;
import com.finovara.reportservice.savingsplan.model.result.SavingsPlanDecision;
import com.finovara.reportservice.savingsplan.model.result.SavingsPlanResult;
import com.finovara.reportservice.savingsplan.model.amount.SavingsPlanAmounts;
import com.finovara.reportservice.savingsplan.model.history.SavingsPlanStats;

import java.math.BigDecimal;
import java.util.List;

public record SavingsPlanInput(SavingsPlanRequest request, SavingsPlanStats stats, SavingsPlanAmounts amounts) {

    private static final BigDecimal HIGH_PAYMENT_RATE = new BigDecimal("0.7");

    public SavingsPlanResult result(BigDecimal moneySavedPerMonth, GoalResult result, List<PlanOption> options, String message) {
        BigDecimal highPaymentLimit = stats.monthlyRevenue().multiply(HIGH_PAYMENT_RATE);
        boolean highPaymentWarning = amounts.moneyNeededPerMonth().compareTo(highPaymentLimit) > 0;
        SavingsPlanDecision decision = new SavingsPlanDecision(result, options, highPaymentWarning, message);
        return new SavingsPlanResult(amounts, moneySavedPerMonth, decision);
    }

    public BigDecimal amountStillNeededForGoal() {
        return amounts.amountStillNeededForGoal();
    }

    public int monthsToTarget() {
        return amounts.monthsToTarget();
    }

    public BigDecimal moneyNeededPerMonth() {
        return amounts.moneyNeededPerMonth();
    }

    public BigDecimal moneyAvailablePerMonth() {
        return amounts.moneyAvailablePerMonth();
    }
}
