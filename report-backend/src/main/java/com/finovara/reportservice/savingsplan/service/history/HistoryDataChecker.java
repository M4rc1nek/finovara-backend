package com.finovara.reportservice.savingsplan.service.history;

import com.finovara.contracts.mainaccount.report.savingsplan.dto.SavingsPlanMonthDto;
import com.finovara.contracts.util.model.ExpenseCategory;
import com.finovara.reportservice.savingsplan.dto.response.model.DataQuality;
import com.finovara.reportservice.savingsplan.service.money.NumberMath;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
@RequiredArgsConstructor
public class HistoryDataChecker {

    static final int LOW_MONTH_COUNT = 3;
    static final int HIGH_MONTH_COUNT = 5;
    static final BigDecimal MAX_CHANGE_RATE = new BigDecimal("0.6");

    private final NumberMath numberMath;

    public DataQuality checkHistoryQuality(List<SavingsPlanMonthDto> monthlyData, List<BigDecimal> monthlyRevenues) {
        long monthsWithData = countMonthsWithData(monthlyData);
        BigDecimal largestChange = numberMath.calculateChangeRate(monthlyRevenues)
                .max(numberMath.calculateChangeRate(monthlyExpenseTotals(monthlyData)));

        for (ExpenseCategory category : ExpenseCategory.values()) {
            List<BigDecimal> categoryExpenses = monthlyData.stream()
                    .map(month -> month.expenses().getOrDefault(category, BigDecimal.ZERO))
                    .toList();
            largestChange = largestChange.max(numberMath.calculateChangeRate(categoryExpenses));
        }

        if (monthsWithData < LOW_MONTH_COUNT || largestChange.compareTo(MAX_CHANGE_RATE) > 0) {
            return DataQuality.LOW;
        }
        return monthsWithData >= HIGH_MONTH_COUNT ? DataQuality.HIGH : DataQuality.MEDIUM;
    }

    private long countMonthsWithData(List<SavingsPlanMonthDto> monthlyData) {
        return monthlyData.stream()
                .filter(month -> month.revenue().signum() > 0
                        || month.expenses().values().stream().anyMatch(amount -> amount.signum() > 0))
                .count();
    }

    private List<BigDecimal> monthlyExpenseTotals(List<SavingsPlanMonthDto> monthlyData) {
        return monthlyData.stream()
                .map(month -> month.expenses().values().stream().reduce(BigDecimal.ZERO, BigDecimal::add))
                .toList();
    }
}
