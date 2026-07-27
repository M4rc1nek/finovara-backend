package com.finovara.financeservice.util.piggybank.goalplanner.calculator;

import com.finovara.financeservice.piggybank.goalplanner.model.GoalPlanner;
import com.finovara.financeservice.piggybank.model.PiggyBank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

class GoalPlannerCalculatorTest {

    private PiggyBank piggyBank;
    private GoalPlanner goalPlanner;

    @BeforeEach
    void setUp() {
        piggyBank = PiggyBank.builder()
                .amount(new BigDecimal("200.00"))
                .goalAmount(new BigDecimal("1000.00"))
                .build();

        goalPlanner = GoalPlanner.builder()
                .piggyBankAssigned(piggyBank)
                .targetDate(LocalDate.now().plusDays(10))
                .build();
    }

    @Nested
    class CalculateRemainingAmount {

        @Test
        void shouldReturnDifferenceWhenDepositedLessThanGoal() {
            BigDecimal result = GoalPlannerCalculator.calculateRemainingAmount(goalPlanner);

            assertEquals(new BigDecimal("800.00"), result);
        }

        @Test
        void shouldReturnZeroWhenDepositedEqualsGoal() {
            piggyBank.setAmount(new BigDecimal("1000.00"));

            BigDecimal result = GoalPlannerCalculator.calculateRemainingAmount(goalPlanner);

            assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        void shouldReturnZeroWhenDepositedExceedsGoal() {
            piggyBank.setAmount(new BigDecimal("1500.00"));

            BigDecimal result = GoalPlannerCalculator.calculateRemainingAmount(goalPlanner);

            assertEquals(BigDecimal.ZERO, result);
        }

        @Test
        void shouldTreatNullDepositedAmountAsZero() {
            piggyBank.setAmount(null);

            BigDecimal result = GoalPlannerCalculator.calculateRemainingAmount(goalPlanner);

            assertEquals(new BigDecimal("1000.00"), result);
        }
    }

    @Nested
    class CalculateDailyInstallment {

        @Test
        void shouldDivideRemainingAmountByDaysLeftWhenPeriodsPositive() {
            goalPlanner.setTargetDate(LocalDate.now().plusDays(10));

            BigDecimal result = GoalPlannerCalculator.calculateDailyInstallment(goalPlanner);

            assertEquals(new BigDecimal("80.00"), result);
        }

        @Test
        void shouldReturnRemainingAmountWhenTargetDateIsToday() {
            goalPlanner.setTargetDate(LocalDate.now());

            BigDecimal result = GoalPlannerCalculator.calculateDailyInstallment(goalPlanner);

            assertEquals(new BigDecimal("800.00"), result);
        }

        @Test
        void shouldReturnRemainingAmountWhenTargetDateInPast() {
            goalPlanner.setTargetDate(LocalDate.now().minusDays(5));

            BigDecimal result = GoalPlannerCalculator.calculateDailyInstallment(goalPlanner);

            assertEquals(new BigDecimal("800.00"), result);
        }

        @Test
        void shouldRoundHalfUpWhenDivisionIsNotExact() {
            goalPlanner.setTargetDate(LocalDate.now().plusDays(3));

            BigDecimal result = GoalPlannerCalculator.calculateDailyInstallment(goalPlanner);

            assertEquals(new BigDecimal("266.67"), result);
        }

        @Test
        void shouldReturnZeroWhenRemainingAmountIsZero() {
            piggyBank.setAmount(new BigDecimal("1000.00"));
            goalPlanner.setTargetDate(LocalDate.now().plusDays(10));

            BigDecimal result = GoalPlannerCalculator.calculateDailyInstallment(goalPlanner);

            assertEquals(BigDecimal.ZERO.setScale(2), result);
        }
    }

    @Nested
    class CalculateWeeklyInstallment {

        @Test
        void shouldDivideRemainingAmountByWeeksLeftWhenPeriodsPositive() {
            goalPlanner.setTargetDate(LocalDate.now().plusWeeks(2));

            BigDecimal result = GoalPlannerCalculator.calculateWeeklyInstallment(goalPlanner);

            assertEquals(new BigDecimal("400.00"), result);
        }

        @Test
        void shouldReturnRemainingAmountWhenTargetDateIsToday() {
            goalPlanner.setTargetDate(LocalDate.now());

            BigDecimal result = GoalPlannerCalculator.calculateWeeklyInstallment(goalPlanner);

            assertEquals(new BigDecimal("800.00"), result);
        }

        @Test
        void shouldReturnRemainingAmountWhenTargetDateInPast() {
            goalPlanner.setTargetDate(LocalDate.now().minusWeeks(1));

            BigDecimal result = GoalPlannerCalculator.calculateWeeklyInstallment(goalPlanner);

            assertEquals(new BigDecimal("800.00"), result);
        }
    }

    @Nested
    class CalculateMonthlyInstallment {

        @Test
        void shouldDivideRemainingAmountByMonthsLeftWhenPeriodsPositive() {
            goalPlanner.setTargetDate(LocalDate.now().plusMonths(4));

            BigDecimal result = GoalPlannerCalculator.calculateMonthlyInstallment(goalPlanner);

            assertEquals(new BigDecimal("200.00"), result);
        }

        @Test
        void shouldReturnRemainingAmountWhenTargetDateIsToday() {
            goalPlanner.setTargetDate(LocalDate.now());

            BigDecimal result = GoalPlannerCalculator.calculateMonthlyInstallment(goalPlanner);

            assertEquals(new BigDecimal("800.00"), result);
        }

        @Test
        void shouldReturnRemainingAmountWhenTargetDateInPast() {
            goalPlanner.setTargetDate(LocalDate.now().minusMonths(2));

            BigDecimal result = GoalPlannerCalculator.calculateMonthlyInstallment(goalPlanner);

            assertEquals(new BigDecimal("800.00"), result);
        }
    }
}