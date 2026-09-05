package com.finovara.reportservice.healthscore.service;

import com.finovara.contracts.transaction.report.dto.HighestExpenseDto;
import com.finovara.reportservice.feignclient.FinanceBackendReportClient;
import com.finovara.reportservice.healthscore.dto.HealthScoreDto;
import com.finovara.reportservice.healthscore.model.HealthScoreStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class HealthScoreService {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final BigDecimal SAVINGS_TARGET_RATE = BigDecimal.valueOf(30);
    private static final BigDecimal EMERGENCY_FUND_TARGET_MONTHS = BigDecimal.valueOf(3);
    private static final int EXPENSE_DIVERSITY_SAMPLE_SIZE = 5;

    private static final BigDecimal SAVINGS_WEIGHT = BigDecimal.valueOf(0.40);
    private static final BigDecimal EMERGENCY_FUND_WEIGHT = BigDecimal.valueOf(0.35);
    private static final BigDecimal EXPENSE_DIVERSITY_WEIGHT = BigDecimal.valueOf(0.25);

    private final FinanceBackendReportClient reportClient;

    @Cacheable(value = "healthScore", key = "#userId")
    public HealthScoreDto getHealthScore(Long userId) {
        LocalDate to = LocalDate.now();
        LocalDate from = to.withDayOfMonth(1);

        int savingsScore = calculateSavingsScore(userId, from, to);
        int emergencyFundScore = calculateEmergencyFundScore(userId, from, to);
        int expenseDiversityScore = calculateExpenseDiversityScore(userId, from, to);

        int finalScore = aggregateScores(savingsScore, emergencyFundScore, expenseDiversityScore);
        HealthScoreStatus status = statusFromScore(finalScore);

        return new HealthScoreDto(BigDecimal.valueOf(finalScore), status);
    }

    private int aggregateScores(int savingsScore, int emergencyFundScore, int expenseDiversityScore) {
        BigDecimal weightedSum = BigDecimal.valueOf(savingsScore).multiply(SAVINGS_WEIGHT)
                .add(BigDecimal.valueOf(emergencyFundScore).multiply(EMERGENCY_FUND_WEIGHT))
                .add(BigDecimal.valueOf(expenseDiversityScore).multiply(EXPENSE_DIVERSITY_WEIGHT));

        return weightedSum.setScale(0, RoundingMode.HALF_UP).intValue();
    }

    private int calculateSavingsScore(Long userId, LocalDate from, LocalDate to) {
        BigDecimal revenues = reportClient.sumRevenues(userId, from, to);

        if (revenues == null || revenues.compareTo(BigDecimal.ZERO) == 0) {
            return 0;
        }

        BigDecimal expenses = valueOrZero(reportClient.sumExpenses(userId, from, to));

        BigDecimal savingsRate = revenues.subtract(expenses)
                .divide(revenues, 4, RoundingMode.HALF_UP)
                .multiply(HUNDRED);

        return scoreScaledToTarget(savingsRate, SAVINGS_TARGET_RATE);
    }

    private int calculateEmergencyFundScore(Long userId, LocalDate from, LocalDate to) {
        BigDecimal walletBalance = valueOrZero(reportClient.walletBalance(userId));
        BigDecimal avgMonthlyExpenses = reportClient.avgExpenses(userId, from, to);

        if (avgMonthlyExpenses == null || avgMonthlyExpenses.compareTo(BigDecimal.ZERO) == 0) {
            return 100;
        }

        BigDecimal monthsCovered = walletBalance.divide(avgMonthlyExpenses, 4, RoundingMode.HALF_UP);

        return scoreScaledToTarget(monthsCovered, EMERGENCY_FUND_TARGET_MONTHS);
    }

    private int calculateExpenseDiversityScore(Long userId, LocalDate from, LocalDate to) {
        BigDecimal totalExpenses = reportClient.sumExpenses(userId, from, to);

        if (totalExpenses == null || totalExpenses.compareTo(BigDecimal.ZERO) == 0) {
            return 100;
        }

        List<HighestExpenseDto> topExpenses = reportClient.highestExpenses(userId, from, to, EXPENSE_DIVERSITY_SAMPLE_SIZE);
        BigDecimal topExpensesSum = sumExpenseAmounts(topExpenses);

        BigDecimal concentrationRatio = topExpensesSum.divide(totalExpenses, 4, RoundingMode.HALF_UP);
        BigDecimal score = HUNDRED.subtract(concentrationRatio.multiply(HUNDRED));

        return Math.clamp(score.intValue(), 0, 100);
    }

    private BigDecimal sumExpenseAmounts(List<HighestExpenseDto> expenses) {
        if (expenses == null || expenses.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return expenses.stream()
                .map(HighestExpenseDto::amount)
                .filter(Objects::nonNull)
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