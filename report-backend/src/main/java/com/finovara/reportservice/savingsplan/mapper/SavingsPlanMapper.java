package com.finovara.reportservice.savingsplan.mapper;

import com.finovara.reportservice.savingsplan.dto.request.SavingsPlanRequest;
import com.finovara.reportservice.savingsplan.dto.response.SavingsPlanReport;
import com.finovara.reportservice.savingsplan.dto.response.model.MonthlyStep;
import com.finovara.reportservice.savingsplan.model.history.SavingsPlanStats;
import com.finovara.reportservice.savingsplan.model.result.SavingsPlanResult;
import com.finovara.reportservice.savingsplan.util.MoneyUtils;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Component
public class SavingsPlanMapper {

    public SavingsPlanReport toReport(SavingsPlanRequest request, SavingsPlanStats stats, SavingsPlanResult result) {
        return new SavingsPlanReport(
                MoneyUtils.round(request.targetAmount()),
                request.targetDate(),
                MoneyUtils.round(request.currentSavings()),
                stats.monthlyRevenue(),
                stats.monthlyExpenses(),
                stats.freeMoneyPerMonth(),
                result.moneyNeededPerMonth(),
                result.moneyAvailablePerMonth(),
                result.moneySavedPerMonth(),
                result.result(),
                stats.historyQuality(),
                List.copyOf(result.options()),
                buildMonthlySteps(request.currentSavings(), result.moneyNeededPerMonth(), result.monthsToTarget()),
                result.highPaymentWarning(),
                result.message()
        );
    }

    private List<MonthlyStep> buildMonthlySteps(BigDecimal currentSavings, BigDecimal moneyNeededPerMonth, int monthsToTarget) {
        List<MonthlyStep> steps = new ArrayList<>(monthsToTarget);
        for (int month = 1; month <= monthsToTarget; month++) {
            BigDecimal expectedSavings = currentSavings.add(
                    moneyNeededPerMonth.multiply(BigDecimal.valueOf(month)));
            steps.add(new MonthlyStep(month, MoneyUtils.round(expectedSavings)));
        }
        return List.copyOf(steps);
    }
}
