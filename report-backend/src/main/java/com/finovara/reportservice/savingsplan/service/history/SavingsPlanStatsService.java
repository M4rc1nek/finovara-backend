package com.finovara.reportservice.savingsplan.service.history;

import com.finovara.contracts.mainaccount.report.savingsplan.dto.SavingsPlanMonthDto;
import com.finovara.reportservice.savingsplan.dto.request.RevenueType;
import com.finovara.reportservice.savingsplan.dto.response.model.DataQuality;
import com.finovara.reportservice.savingsplan.model.history.SavingsPlanStats;
import com.finovara.reportservice.savingsplan.service.money.NumberMath;
import com.finovara.reportservice.savingsplan.util.MoneyUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SavingsPlanStatsService {

    private final NumberMath numberMath;
    private final HistoryDataChecker historyDataChecker;

    public SavingsPlanStats calculateStats(List<SavingsPlanMonthDto> monthlyData, RevenueType revenueType) {
        List<BigDecimal> monthlyRevenues = monthlyData.stream()
                .map(SavingsPlanMonthDto::revenue)
                .toList();

        BigDecimal monthlyRevenue = calculateMonthlyRevenue(monthlyRevenues, revenueType);
        List<BigDecimal> monthlyExpenses = monthlyData.stream()
                .map(month -> month.expenses().values().stream().reduce(BigDecimal.ZERO, BigDecimal::add))
                .toList();
        BigDecimal averageMonthlyExpenses = numberMath.calculateAverageAmount(monthlyExpenses);

        BigDecimal freeMoneyPerMonth = MoneyUtils.round(monthlyRevenue.subtract(averageMonthlyExpenses));
        DataQuality dataQuality = historyDataChecker.checkHistoryQuality(monthlyData, monthlyRevenues);

        return new SavingsPlanStats(MoneyUtils.round(monthlyRevenue), averageMonthlyExpenses, freeMoneyPerMonth, dataQuality);
    }

    public BigDecimal calculateMonthlyRevenue(List<BigDecimal> monthlyRevenues, RevenueType revenueType) {
        return revenueType == RevenueType.STABLE ? numberMath.calculateTypicalAmount(monthlyRevenues) : numberMath.calculateLowRevenue(monthlyRevenues);
    }

}
