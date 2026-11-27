package com.finovara.reportservice.savingsplan.dto.response;

import com.finovara.reportservice.savingsplan.dto.response.model.DataQuality;
import com.finovara.reportservice.savingsplan.dto.response.model.GoalResult;
import com.finovara.reportservice.savingsplan.dto.response.model.MonthlyStep;
import com.finovara.reportservice.savingsplan.dto.response.model.PlanOption;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record SavingsPlanReport(
        BigDecimal targetAmount,
        LocalDate targetDate,
        BigDecimal currentSavings,
        BigDecimal monthlyRevenue,
        BigDecimal monthlyExpenses,
        BigDecimal extraMoney,
        BigDecimal neededPerMonth,
        BigDecimal availablePerMonth,
        BigDecimal savingsPerMonth,
        GoalResult result,
        DataQuality dataQuality,
        List<PlanOption> options,
        List<MonthlyStep> monthlySteps,
        boolean highPaymentWarning,
        String message
) {
}
