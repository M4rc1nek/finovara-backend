package com.finovara.reportservice.healthscore.service;

import com.finovara.contracts.mainaccount.report.financial.dto.FinancialReportDto;
import com.finovara.contracts.mainaccount.report.financial.dto.FinancialTotalsDto;
import com.finovara.contracts.mainaccount.report.financial.dto.HighestExpenseDto;
import com.finovara.contracts.util.PeriodType;
import com.finovara.reportservice.feignclient.FinanceBackendReportClient;
import com.finovara.reportservice.healthscore.dto.HealthScoreDto;
import com.finovara.reportservice.healthscore.model.HealthScoreStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class HealthScoreService {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final BigDecimal SAVINGS_TARGET_RATE = BigDecimal.valueOf(30);
    private static final BigDecimal EMERGENCY_FUND_TARGET_MONTHS = BigDecimal.valueOf(3);
    private static final int EXPENSE_DIVERSITY_SAMPLE_SIZE = 5;
    private static final PeriodType REPORT_PERIOD = PeriodType.MONTHLY;

    private static final BigDecimal SAVINGS_WEIGHT = BigDecimal.valueOf(0.40);
    private static final BigDecimal EMERGENCY_FUND_WEIGHT = BigDecimal.valueOf(0.35);
    private static final BigDecimal EXPENSE_DIVERSITY_WEIGHT = BigDecimal.valueOf(0.25);

    private final FinanceBackendReportClient financeBackendReportClient;

    @Cacheable(value = "healthScore", key = "#userId")
    public HealthScoreDto getHealthScore(Long userId) {
        FinancialReportDto report = financeBackendReportClient.getFinancialReport(userId, REPORT_PERIOD);
        BigDecimal walletBalance = valueOrZero(financeBackendReportClient.walletBalance(userId));

        BigDecimal revenues = totalRevenues(report);
        BigDecimal expenses = totalExpenses(report);

        int savingsScore = calculateSavingsScore(revenues, expenses);
        int emergencyFundScore = calculateEmergencyFundScore(walletBalance, expenses);
        int expenseDiversityScore = calculateExpenseDiversityScore(expenses, report);

        int finalScore = aggregateScores(savingsScore, emergencyFundScore, expenseDiversityScore);
        HealthScoreStatus status = statusFromScore(finalScore);

        return new HealthScoreDto(BigDecimal.valueOf(finalScore), status);
    }


    private BigDecimal totalRevenues(FinancialReportDto report) {
        FinancialTotalsDto totals = report == null ? null : report.totals();
        return totals == null ? BigDecimal.ZERO : valueOrZero(totals.revenues());
    }

    private BigDecimal totalExpenses(FinancialReportDto report) {
        FinancialTotalsDto totals = report == null ? null : report.totals();
        return totals == null ? BigDecimal.ZERO : valueOrZero(totals.expenses());
    }


    private int aggregateScores(int savingsScore, int emergencyFundScore, int expenseDiversityScore) {
        BigDecimal weightedSum = BigDecimal.valueOf(savingsScore).multiply(SAVINGS_WEIGHT)
                .add(BigDecimal.valueOf(emergencyFundScore).multiply(EMERGENCY_FUND_WEIGHT))
                .add(BigDecimal.valueOf(expenseDiversityScore).multiply(EXPENSE_DIVERSITY_WEIGHT));

        return weightedSum.setScale(0, RoundingMode.HALF_UP).intValue();
    }

    private int calculateSavingsScore(BigDecimal revenues, BigDecimal expenses) {
        if (revenues.compareTo(BigDecimal.ZERO) == 0) {
            return 0;
        }

        BigDecimal savingsRate = revenues.subtract(expenses)
                .divide(revenues, 4, RoundingMode.HALF_UP)
                .multiply(HUNDRED);

        return scoreScaledToTarget(savingsRate, SAVINGS_TARGET_RATE);
    }

    private int calculateEmergencyFundScore(BigDecimal walletBalance, BigDecimal monthlyExpenses) {
        if (monthlyExpenses.compareTo(BigDecimal.ZERO) == 0) {
            return 100;
        }

        BigDecimal monthsCovered = walletBalance.divide(monthlyExpenses, 4, RoundingMode.HALF_UP);

        return scoreScaledToTarget(monthsCovered, EMERGENCY_FUND_TARGET_MONTHS);
    }

    private int calculateExpenseDiversityScore(BigDecimal totalExpenses, FinancialReportDto report) {
        if (totalExpenses.compareTo(BigDecimal.ZERO) == 0) {
            return 100;
        }

        BigDecimal topExpensesSum = sumTopExpenses(report == null ? null : report.highestExpenses());

        BigDecimal concentrationRatio = topExpensesSum.divide(totalExpenses, 4, RoundingMode.HALF_UP);
        BigDecimal score = HUNDRED.subtract(concentrationRatio.multiply(HUNDRED));

        return Math.clamp(score.intValue(), 0, 100);
    }

    private BigDecimal sumTopExpenses(List<HighestExpenseDto> expenses) {
        if (expenses == null || expenses.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return expenses.stream()
                .map(HighestExpenseDto::amount)
                .filter(Objects::nonNull)
                .sorted(Comparator.reverseOrder())
                .limit(EXPENSE_DIVERSITY_SAMPLE_SIZE)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private int scoreScaledToTarget(BigDecimal value, BigDecimal target) {
        if (value.compareTo(BigDecimal.ZERO) <= 0) {
            return 0;
        }
        if (value.compareTo(target) >= 0) {
            return 100;
        }
        return value.divide(target, 4, RoundingMode.HALF_UP)
                .multiply(HUNDRED)
                .intValue();
    }

    private BigDecimal valueOrZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private HealthScoreStatus statusFromScore(int score) {
        if (score >= 85) return HealthScoreStatus.EXCELLENT;
        if (score >= 70) return HealthScoreStatus.GOOD;
        if (score >= 50) return HealthScoreStatus.NEEDS_ATTENTION;
        return HealthScoreStatus.POOR;
    }
}