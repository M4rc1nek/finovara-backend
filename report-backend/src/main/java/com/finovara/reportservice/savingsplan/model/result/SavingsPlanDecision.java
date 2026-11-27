package com.finovara.reportservice.savingsplan.model.result;

import com.finovara.reportservice.savingsplan.dto.response.model.GoalResult;
import com.finovara.reportservice.savingsplan.dto.response.model.PlanOption;

import java.util.List;

public record SavingsPlanDecision(
        GoalResult result,
        List<PlanOption> options,
        boolean highPaymentWarning,
        String message
) {
}
