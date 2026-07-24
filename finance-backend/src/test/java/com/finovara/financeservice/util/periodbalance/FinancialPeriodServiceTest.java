package com.finovara.financeservice.util.periodbalance;

import com.finovara.contracts.model.PeriodType;
import com.finovara.contracts.model.transaction.ExpenseCategory;
import com.finovara.contracts.model.transaction.RevenueCategory;
import com.finovara.financeservice.expense.model.Expense;
import com.finovara.financeservice.expense.repository.ExpenseRepository;
import com.finovara.financeservice.revenue.model.Revenue;
import com.finovara.financeservice.revenue.repository.RevenueRepository;
import com.finovara.financeservice.sharedaccount.expense.repository.SharedExpenseRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FinancialPeriodServiceTest {

    private static final Long USER_ID = 1L;

    @Mock
    private ExpenseRepository expenseRepository;

    @Mock
    private SharedExpenseRepository sharedExpenseRepository;

    @Mock
    private RevenueRepository revenueRepository;

    @Mock
    private ExpenseCategory expenseCategory;

    @Mock
    private RevenueCategory revenueCategory;

    @InjectMocks
    private FinancialPeriodService financialPeriodService;

    @Nested
    class GetExpensesInPeriodByCategory {

        @ParameterizedTest
        @EnumSource(PeriodType.class)
        void shouldReturnExpensesForEveryPeriodType(PeriodType periodType) {
            LocalDate today = LocalDate.now();
            LocalDate from = periodType.getStartDate(today);
            List<Expense> expected = List.of(mock(Expense.class));

            when(expenseRepository.findAllByUserIdAndCreatedAtBetweenAndCategory(USER_ID, from, today, expenseCategory))
                    .thenReturn(expected);

            List<Expense> result = financialPeriodService.getExpensesInPeriodByCategory(USER_ID, periodType, expenseCategory);

            assertThat(result).isSameAs(expected);
        }

        @Test
        void shouldReturnEmptyListWhenNoExpensesFound() {
            LocalDate today = LocalDate.now();
            LocalDate from = PeriodType.MONTHLY.getStartDate(today);

            when(expenseRepository.findAllByUserIdAndCreatedAtBetweenAndCategory(USER_ID, from, today, expenseCategory))
                    .thenReturn(List.of());

            List<Expense> result = financialPeriodService.getExpensesInPeriodByCategory(USER_ID, PeriodType.MONTHLY, expenseCategory);

            assertThat(result).isEmpty();
        }

        @Test
        void shouldCallRepositoryWithCorrectDateRange() {
            LocalDate today = LocalDate.now();
            LocalDate from = PeriodType.WEEKLY.getStartDate(today);

            when(expenseRepository.findAllByUserIdAndCreatedAtBetweenAndCategory(USER_ID, from, today, expenseCategory))
                    .thenReturn(List.of());

            financialPeriodService.getExpensesInPeriodByCategory(USER_ID, PeriodType.WEEKLY, expenseCategory);

            verify(expenseRepository).findAllByUserIdAndCreatedAtBetweenAndCategory(USER_ID, from, today, expenseCategory);
        }
    }

    @Nested
    class GetRevenuesInPeriodByCategory {

        @ParameterizedTest
        @EnumSource(PeriodType.class)
        void shouldReturnRevenuesForEveryPeriodType(PeriodType periodType) {
            LocalDate today = LocalDate.now();
            LocalDate from = periodType.getStartDate(today);
            List<Revenue> expected = List.of(mock(Revenue.class));

            when(revenueRepository.findAllByUserIdAndCreatedAtBetweenAndCategory(USER_ID, from, today, revenueCategory))
                    .thenReturn(expected);

            List<Revenue> result = financialPeriodService.getRevenuesInPeriodByCategory(USER_ID, periodType, revenueCategory);

            assertThat(result).isSameAs(expected);
        }

        @Test
        void shouldReturnEmptyListWhenNoRevenuesFound() {
            LocalDate today = LocalDate.now();
            LocalDate from = PeriodType.DAILY.getStartDate(today);

            when(revenueRepository.findAllByUserIdAndCreatedAtBetweenAndCategory(USER_ID, from, today, revenueCategory))
                    .thenReturn(List.of());

            List<Revenue> result = financialPeriodService.getRevenuesInPeriodByCategory(USER_ID, PeriodType.DAILY, revenueCategory);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    class GetExpensesSum {

        @Test
        void shouldReturnCategorySumWhenCategoryProvided() {
            LocalDate to = LocalDate.now();
            LocalDate from = PeriodType.MONTHLY.getStartDate(to);

            when(expenseRepository.sumExpensesByUserAndDateRangeAndCategory(USER_ID, from, to, expenseCategory))
                    .thenReturn(Optional.of(new BigDecimal("150.00")));

            BigDecimal result = financialPeriodService.getExpensesSum(USER_ID, PeriodType.MONTHLY, expenseCategory);

            assertThat(result).isEqualByComparingTo("150.00");
            verify(expenseRepository, never()).sumExpensesByUserAndDateRange(USER_ID, from, to);
        }

        @Test
        void shouldReturnGeneralSumWhenCategoryIsNull() {
            LocalDate to = LocalDate.now();
            LocalDate from = PeriodType.MONTHLY.getStartDate(to);

            when(expenseRepository.sumExpensesByUserAndDateRange(USER_ID, from, to))
                    .thenReturn(Optional.of(new BigDecimal("500.00")));

            BigDecimal result = financialPeriodService.getExpensesSum(USER_ID, PeriodType.MONTHLY, null);

            assertThat(result).isEqualByComparingTo("500.00");
            verify(expenseRepository, never()).sumExpensesByUserAndDateRangeAndCategory(USER_ID, from, to, expenseCategory);
        }

        @Test
        void shouldReturnZeroWhenGeneralSumIsEmpty() {
            LocalDate to = LocalDate.now();
            LocalDate from = PeriodType.DAILY.getStartDate(to);

            when(expenseRepository.sumExpensesByUserAndDateRange(USER_ID, from, to)).thenReturn(Optional.empty());

            BigDecimal result = financialPeriodService.getExpensesSum(USER_ID, PeriodType.DAILY, null);

            assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        void shouldReturnZeroWhenCategorySumIsEmpty() {
            LocalDate to = LocalDate.now();
            LocalDate from = PeriodType.DAILY.getStartDate(to);

            when(expenseRepository.sumExpensesByUserAndDateRangeAndCategory(USER_ID, from, to, expenseCategory))
                    .thenReturn(Optional.empty());

            BigDecimal result = financialPeriodService.getExpensesSum(USER_ID, PeriodType.DAILY, expenseCategory);

            assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
        }
    }

    @Nested
    class GetSharedExpensesSum {

        @Test
        void shouldReturnCategorySumWhenCategoryProvided() {
            LocalDate to = LocalDate.now();
            LocalDate from = PeriodType.MONTHLY.getStartDate(to);

            when(sharedExpenseRepository.sumExpensesByUsersAndDateRangeAndCategory(USER_ID, from, to, expenseCategory))
                    .thenReturn(Optional.of(new BigDecimal("220.00")));

            BigDecimal result = financialPeriodService.getSharedExpensesSum(USER_ID, PeriodType.MONTHLY, expenseCategory);

            assertThat(result).isEqualByComparingTo("220.00");
            verify(sharedExpenseRepository, never()).sumExpensesByUsersAndDateRange(USER_ID, from, to);
        }

        @Test
        void shouldReturnGeneralSumWhenCategoryIsNull() {
            LocalDate to = LocalDate.now();
            LocalDate from = PeriodType.WEEKLY.getStartDate(to);

            when(sharedExpenseRepository.sumExpensesByUsersAndDateRange(USER_ID, from, to))
                    .thenReturn(Optional.of(new BigDecimal("640.00")));

            BigDecimal result = financialPeriodService.getSharedExpensesSum(USER_ID, PeriodType.WEEKLY, null);

            assertThat(result).isEqualByComparingTo("640.00");
            verify(sharedExpenseRepository, never()).sumExpensesByUsersAndDateRangeAndCategory(USER_ID, from, to, expenseCategory);
        }

        @Test
        void shouldReturnZeroWhenGeneralSumIsEmpty() {
            LocalDate to = LocalDate.now();
            LocalDate from = PeriodType.DAILY.getStartDate(to);

            when(sharedExpenseRepository.sumExpensesByUsersAndDateRange(USER_ID, from, to)).thenReturn(Optional.empty());

            BigDecimal result = financialPeriodService.getSharedExpensesSum(USER_ID, PeriodType.DAILY, null);

            assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        void shouldReturnZeroWhenCategorySumIsEmpty() {
            LocalDate to = LocalDate.now();
            LocalDate from = PeriodType.MONTHLY.getStartDate(to);

            when(sharedExpenseRepository.sumExpensesByUsersAndDateRangeAndCategory(USER_ID, from, to, expenseCategory))
                    .thenReturn(Optional.empty());

            BigDecimal result = financialPeriodService.getSharedExpensesSum(USER_ID, PeriodType.MONTHLY, expenseCategory);

            assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
        }
    }
}