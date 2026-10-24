package com.finovara.financeservice.util.transaction.piggybank.goalplanner.calculator;

import com.finovara.contracts.util.PeriodType;
import com.finovara.financeservice.piggybank.goalplanner.model.GoalPlanner;
import com.finovara.financeservice.piggybank.model.PiggyBank;
import lombok.experimental.UtilityClass;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@UtilityClass
public class GoalPlannerCalculator {

    public static BigDecimal calculateRemainingAmount(PiggyBank piggyBank) {
        BigDecimal deposited = piggyBank.getAmount() == null
                ? BigDecimal.ZERO
                : piggyBank.getAmount();

        BigDecimal goalAmount = piggyBank.getGoalAmount() == null
                ? BigDecimal.ZERO
                : piggyBank.getGoalAmount();

        return goalAmount.subtract(deposited).max(BigDecimal.ZERO);
    }

    public static BigDecimal calculateDailyInstallment(GoalPlanner goalPlanner, PiggyBank piggyBank) {
        return calculateInstallment(goalPlanner, piggyBank, PeriodType.DAILY);
    }

    public static BigDecimal calculateWeeklyInstallment(GoalPlanner goalPlanner, PiggyBank piggyBank) {
        return calculateInstallment(goalPlanner, piggyBank, PeriodType.WEEKLY);
    }

    public static BigDecimal calculateMonthlyInstallment(GoalPlanner goalPlanner, PiggyBank piggyBank) {
        return calculateInstallment(goalPlanner, piggyBank, PeriodType.MONTHLY);
    }

    private static BigDecimal calculateInstallment(GoalPlanner goalPlanner, PiggyBank piggyBank, PeriodType periodType) {
        BigDecimal remainingAmount = calculateRemainingAmount(piggyBank);
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