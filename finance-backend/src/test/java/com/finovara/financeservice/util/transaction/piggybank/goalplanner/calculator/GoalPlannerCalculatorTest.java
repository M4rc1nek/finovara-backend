package com.finovara.financeservice.util.transaction.piggybank.goalplanner.calculator;

import com.finovara.financeservice.piggybank.goalplanner.model.GoalPlanner;
import com.finovara.financeservice.piggybank.model.PiggyBank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GoalPlannerCalculatorTest {

    private static final BigDecimal GOAL_AMOUNT = BigDecimal.valueOf(1000);
    private static final BigDecimal DEPOSITED_AMOUNT = BigDecimal.valueOf(400);

    @Mock
    private PiggyBank piggyBank;

    @Mock
    private GoalPlanner goalPlanner;

    @Nested
    class CalculateRemainingAmount {

        @Test
        void shouldReturnDifferenceWhenGoalExceedsDeposited() {
            when(piggyBank.getAmount()).thenReturn(DEPOSITED_AMOUNT);
            when(piggyBank.getGoalAmount()).thenReturn(GOAL_AMOUNT);

            BigDecimal result = GoalPlannerCalculator.calculateRemainingAmount(piggyBank);

            assertEquals(BigDecimal.valueOf(600), result);
        }

        @Test
        void shouldPreserveDecimalPartWhenAmountsHaveFractions() {
            when(piggyBank.getAmount()).thenReturn(new BigDecimal("40.25"));
            when(piggyBank.getGoalAmount()).thenReturn(new BigDecimal("100.50"));

            BigDecimal result = GoalPlannerCalculator.calculateRemainingAmount(piggyBank);

            assertEquals(new BigDecimal("60.25"), result);
        }

        @Test
        void shouldReturnZeroWhenDepositedEqualsGoal() {
            when(piggyBank.getAmount()).thenReturn(GOAL_AMOUNT);
            when(piggyBank.getGoalAmount()).thenReturn(GOAL_AMOUNT);

            BigDecimal result = GoalPlannerCalculator.calculateRemainingAmount(piggyBank);

            assertEquals(0, BigDecimal.ZERO.compareTo(result));
        }

        @Test
        void shouldReturnZeroWhenDepositedExceedsGoal() {
            when(piggyBank.getAmount()).thenReturn(BigDecimal.valueOf(1500));
            when(piggyBank.getGoalAmount()).thenReturn(GOAL_AMOUNT);

            BigDecimal result = GoalPlannerCalculator.calculateRemainingAmount(piggyBank);

            assertEquals(0, BigDecimal.ZERO.compareTo(result));
        }

        @Test
        void shouldReturnGoalAmountWhenDepositedIsNull() {
            when(piggyBank.getGoalAmount()).thenReturn(GOAL_AMOUNT);

            BigDecimal result = GoalPlannerCalculator.calculateRemainingAmount(piggyBank);

            assertEquals(GOAL_AMOUNT, result);
        }

        @Test
        void shouldReturnZeroWhenGoalAmountIsNull() {
            when(piggyBank.getAmount()).thenReturn(DEPOSITED_AMOUNT);

            BigDecimal result = GoalPlannerCalculator.calculateRemainingAmount(piggyBank);

            assertEquals(0, BigDecimal.ZERO.compareTo(result));
        }

        @Test
        void shouldReturnZeroWhenBothAmountsAreNull() {
            BigDecimal result = GoalPlannerCalculator.calculateRemainingAmount(piggyBank);

            assertEquals(0, BigDecimal.ZERO.compareTo(result));
        }
    }

    @Nested
    class CalculateDailyInstallment {

        @BeforeEach
        void setUp() {
            when(piggyBank.getGoalAmount()).thenReturn(GOAL_AMOUNT);
        }

        @Test
        void shouldDivideRemainingAmountByDaysLeftWhenTargetDateInFuture() {
            when(piggyBank.getAmount()).thenReturn(DEPOSITED_AMOUNT);
            when(goalPlanner.getTargetDate()).thenReturn(LocalDate.now().plusDays(10));

            BigDecimal result = GoalPlannerCalculator.calculateDailyInstallment(goalPlanner, piggyBank);

            assertEquals(new BigDecimal("60.00"), result);
        }

        @Test
        void shouldRoundHalfUpWhenDivisionIsNotExact() {
            when(piggyBank.getAmount()).thenReturn(BigDecimal.valueOf(800));
            when(goalPlanner.getTargetDate()).thenReturn(LocalDate.now().plusDays(3));

            BigDecimal result = GoalPlannerCalculator.calculateDailyInstallment(goalPlanner, piggyBank);

            assertEquals(new BigDecimal("66.67"), result);
        }

        @Test
        void shouldRoundHalfUpDownWhenRemainderIsBelowHalf() {
            when(piggyBank.getAmount()).thenReturn(BigDecimal.valueOf(900));
            when(goalPlanner.getTargetDate()).thenReturn(LocalDate.now().plusDays(3));

            BigDecimal result = GoalPlannerCalculator.calculateDailyInstallment(goalPlanner, piggyBank);

            assertEquals(new BigDecimal("33.33"), result);
        }

        @Test
        void shouldReturnWholeRemainingAmountWhenOneDayLeft() {
            when(piggyBank.getAmount()).thenReturn(DEPOSITED_AMOUNT);
            when(goalPlanner.getTargetDate()).thenReturn(LocalDate.now().plusDays(1));

            BigDecimal result = GoalPlannerCalculator.calculateDailyInstallment(goalPlanner, piggyBank);

            assertEquals(new BigDecimal("600.00"), result);
        }

        @Test
        void shouldReturnRemainingAmountWhenTargetDateIsToday() {
            when(piggyBank.getAmount()).thenReturn(DEPOSITED_AMOUNT);
            when(goalPlanner.getTargetDate()).thenReturn(LocalDate.now());

            BigDecimal result = GoalPlannerCalculator.calculateDailyInstallment(goalPlanner, piggyBank);

            assertEquals(BigDecimal.valueOf(600), result);
        }

        @Test
        void shouldReturnRemainingAmountWhenTargetDateInPast() {
            when(piggyBank.getAmount()).thenReturn(DEPOSITED_AMOUNT);
            when(goalPlanner.getTargetDate()).thenReturn(LocalDate.now().minusDays(5));

            BigDecimal result = GoalPlannerCalculator.calculateDailyInstallment(goalPlanner, piggyBank);

            assertEquals(BigDecimal.valueOf(600), result);
        }

        @Test
        void shouldReturnZeroWhenGoalAlreadyReached() {
            when(piggyBank.getAmount()).thenReturn(GOAL_AMOUNT);
            when(goalPlanner.getTargetDate()).thenReturn(LocalDate.now().plusDays(10));

            BigDecimal result = GoalPlannerCalculator.calculateDailyInstallment(goalPlanner, piggyBank);

            assertEquals(0, BigDecimal.ZERO.compareTo(result));
        }

        @Test
        void shouldThrowExceptionWhenTargetDateIsNull() {
            when(piggyBank.getAmount()).thenReturn(DEPOSITED_AMOUNT);

            assertThrows(NullPointerException.class, () -> GoalPlannerCalculator.calculateDailyInstallment(goalPlanner, piggyBank));
        }
    }

    @Nested
    class CalculateWeeklyInstallment {

        @BeforeEach
        void setUp() {
            when(piggyBank.getGoalAmount()).thenReturn(GOAL_AMOUNT);
        }

        @Test
        void shouldDivideRemainingAmountByWeeksLeftWhenTargetDateInFuture() {
            when(piggyBank.getAmount()).thenReturn(DEPOSITED_AMOUNT);
            when(goalPlanner.getTargetDate()).thenReturn(LocalDate.now().plusWeeks(3));

            BigDecimal result = GoalPlannerCalculator.calculateWeeklyInstallment(goalPlanner, piggyBank);

            assertEquals(new BigDecimal("200.00"), result);
        }

        @Test
        void shouldIgnoreIncompleteWeekWhenTargetDateIsNotFullWeeksAway() {
            when(piggyBank.getAmount()).thenReturn(DEPOSITED_AMOUNT);
            when(goalPlanner.getTargetDate()).thenReturn(LocalDate.now().plusWeeks(3).plusDays(6));

            BigDecimal result = GoalPlannerCalculator.calculateWeeklyInstallment(goalPlanner, piggyBank);

            assertEquals(new BigDecimal("200.00"), result);
        }

        @Test
        void shouldRoundHalfUpWhenDivisionIsNotExact() {
            when(piggyBank.getAmount()).thenReturn(BigDecimal.valueOf(800));
            when(goalPlanner.getTargetDate()).thenReturn(LocalDate.now().plusWeeks(3));

            BigDecimal result = GoalPlannerCalculator.calculateWeeklyInstallment(goalPlanner, piggyBank);

            assertEquals(new BigDecimal("66.67"), result);
        }

        @Test
        void shouldReturnRemainingAmountWhenLessThanOneWeekLeft() {
            when(piggyBank.getAmount()).thenReturn(DEPOSITED_AMOUNT);
            when(goalPlanner.getTargetDate()).thenReturn(LocalDate.now().plusDays(6));

            BigDecimal result = GoalPlannerCalculator.calculateWeeklyInstallment(goalPlanner, piggyBank);

            assertEquals(BigDecimal.valueOf(600), result);
        }

        @Test
        void shouldReturnWholeRemainingAmountWhenExactlyOneWeekLeft() {
            when(piggyBank.getAmount()).thenReturn(DEPOSITED_AMOUNT);
            when(goalPlanner.getTargetDate()).thenReturn(LocalDate.now().plusWeeks(1));

            BigDecimal result = GoalPlannerCalculator.calculateWeeklyInstallment(goalPlanner, piggyBank);

            assertEquals(new BigDecimal("600.00"), result);
        }

        @Test
        void shouldReturnRemainingAmountWhenTargetDateInPast() {
            when(piggyBank.getAmount()).thenReturn(DEPOSITED_AMOUNT);
            when(goalPlanner.getTargetDate()).thenReturn(LocalDate.now().minusWeeks(2));

            BigDecimal result = GoalPlannerCalculator.calculateWeeklyInstallment(goalPlanner, piggyBank);

            assertEquals(BigDecimal.valueOf(600), result);
        }

        @Test
        void shouldReturnZeroWhenGoalAlreadyReached() {
            when(piggyBank.getAmount()).thenReturn(BigDecimal.valueOf(1200));
            when(goalPlanner.getTargetDate()).thenReturn(LocalDate.now().plusWeeks(4));

            BigDecimal result = GoalPlannerCalculator.calculateWeeklyInstallment(goalPlanner, piggyBank);

            assertEquals(0, BigDecimal.ZERO.compareTo(result));
        }

        @Test
        void shouldThrowExceptionWhenTargetDateIsNull() {
            when(piggyBank.getAmount()).thenReturn(DEPOSITED_AMOUNT);

            assertThrows(NullPointerException.class, () -> GoalPlannerCalculator.calculateWeeklyInstallment(goalPlanner, piggyBank));
        }
    }

    @Nested
    class CalculateMonthlyInstallment {

        @BeforeEach
        void setUp() {
            when(piggyBank.getGoalAmount()).thenReturn(GOAL_AMOUNT);
        }

        @Test
        void shouldDivideRemainingAmountByMonthsLeftWhenTargetDateInFuture() {
            when(piggyBank.getAmount()).thenReturn(DEPOSITED_AMOUNT);
            when(goalPlanner.getTargetDate()).thenReturn(LocalDate.now().plusMonths(2).plusDays(1));

            BigDecimal result = GoalPlannerCalculator.calculateMonthlyInstallment(goalPlanner, piggyBank);

            assertEquals(new BigDecimal("300.00"), result);
        }

        @Test
        void shouldRoundHalfUpWhenDivisionIsNotExact() {
            when(piggyBank.getAmount()).thenReturn(BigDecimal.valueOf(800));
            when(goalPlanner.getTargetDate()).thenReturn(LocalDate.now().plusMonths(3).plusDays(1));

            BigDecimal result = GoalPlannerCalculator.calculateMonthlyInstallment(goalPlanner, piggyBank);

            assertEquals(new BigDecimal("66.67"), result);
        }

        @Test
        void shouldReturnWholeRemainingAmountWhenOneMonthLeft() {
            when(piggyBank.getAmount()).thenReturn(DEPOSITED_AMOUNT);
            when(goalPlanner.getTargetDate()).thenReturn(LocalDate.now().plusMonths(1).plusDays(1));

            BigDecimal result = GoalPlannerCalculator.calculateMonthlyInstallment(goalPlanner, piggyBank);

            assertEquals(new BigDecimal("600.00"), result);
        }

        @Test
        void shouldReturnRemainingAmountWhenLessThanOneMonthLeft() {
            when(piggyBank.getAmount()).thenReturn(DEPOSITED_AMOUNT);
            when(goalPlanner.getTargetDate()).thenReturn(LocalDate.now().plusDays(10));

            BigDecimal result = GoalPlannerCalculator.calculateMonthlyInstallment(goalPlanner, piggyBank);

            assertEquals(BigDecimal.valueOf(600), result);
        }

        @Test
        void shouldReturnRemainingAmountWhenTargetDateIsToday() {
            when(piggyBank.getAmount()).thenReturn(DEPOSITED_AMOUNT);
            when(goalPlanner.getTargetDate()).thenReturn(LocalDate.now());

            BigDecimal result = GoalPlannerCalculator.calculateMonthlyInstallment(goalPlanner, piggyBank);

            assertEquals(BigDecimal.valueOf(600), result);
        }

        @Test
        void shouldReturnRemainingAmountWhenTargetDateInPast() {
            when(piggyBank.getAmount()).thenReturn(DEPOSITED_AMOUNT);
            when(goalPlanner.getTargetDate()).thenReturn(LocalDate.now().minusMonths(3));

            BigDecimal result = GoalPlannerCalculator.calculateMonthlyInstallment(goalPlanner, piggyBank);

            assertEquals(BigDecimal.valueOf(600), result);
        }

        @Test
        void shouldReturnZeroWhenGoalAlreadyReached() {
            when(piggyBank.getAmount()).thenReturn(GOAL_AMOUNT);
            when(goalPlanner.getTargetDate()).thenReturn(LocalDate.now().plusMonths(6).plusDays(1));

            BigDecimal result = GoalPlannerCalculator.calculateMonthlyInstallment(goalPlanner, piggyBank);

            assertEquals(0, BigDecimal.ZERO.compareTo(result));
        }

        @Test
        void shouldThrowExceptionWhenTargetDateIsNull() {
            when(piggyBank.getAmount()).thenReturn(DEPOSITED_AMOUNT);

            assertThrows(NullPointerException.class, () -> GoalPlannerCalculator.calculateMonthlyInstallment(goalPlanner, piggyBank));
        }
    }
}