package com.finovara.financeservice.util.transaction.piggybank.goalplanner.calculator;

import com.finovara.contracts.model.PeriodType;
import com.finovara.financeservice.piggybank.goalplanner.model.GoalPlanner;
import lombok.experimental.UtilityClass;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@UtilityClass
public class GoalPlannerCalculator {

    public static BigDecimal calculateRemainingAmount(GoalPlanner goalPlanner) {
        BigDecimal deposited = goalPlanner.getPiggyBankAssigned().getAmount() == null
                ? BigDecimal.ZERO
                : goalPlanner.getPiggyBankAssigned().getAmount();

        BigDecimal goalAmount = goalPlanner.getPiggyBankAssigned().getGoalAmount();

        return goalAmount.subtract(deposited).max(BigDecimal.ZERO);
    }

    public static BigDecimal calculateDailyInstallment(GoalPlanner goalPlanner) {
        return calculateInstallment(goalPlanner, PeriodType.DAILY);
    }

    public static BigDecimal calculateWeeklyInstallment(GoalPlanner goalPlanner) {
        return calculateInstallment(goalPlanner, PeriodType.WEEKLY);
    }

    public static BigDecimal calculateMonthlyInstallment(GoalPlanner goalPlanner) {
        return calculateInstallment(goalPlanner, PeriodType.MONTHLY);
    }

    private static BigDecimal calculateInstallment(GoalPlanner goalPlanner, PeriodType periodType) {
        BigDecimal remainingAmount = calculateRemainingAmount(goalPlanner);
        long periodsLeft = countPeriods(periodType, LocalDate.now(), goalPlanner.getTargetDate());

        if (periodsLeft <= 0) {
            return remainingAmount;
        }

        return remainingAmount.divide(BigDecimal.valueOf(periodsLeft), 2, RoundingMode.HALF_UP);
    }

    private static long countPeriods(PeriodType periodType, LocalDate today, LocalDate targetDate) {
        return switch (periodType) {
            case DAILY -> ChronoUnit.DAYS.between(today, targetDate);
            case WEEKLY -> ChronoUnit.WEEKS.between(today, targetDate);
            case MONTHLY -> ChronoUnit.MONTHS.between(today, targetDate);
        };
    }
}