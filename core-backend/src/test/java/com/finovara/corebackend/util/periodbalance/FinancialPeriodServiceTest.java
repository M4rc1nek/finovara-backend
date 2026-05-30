package com.finovara.corebackend.util.periodbalance;

import com.finovara.contracts.model.transaction.ExpenseCategory;
import com.finovara.corebackend.expense.repository.ExpenseRepository;
import com.finovara.contracts.model.transaction.RevenueCategory;
import com.finovara.corebackend.revenue.repository.RevenueRepository;
import com.finovara.contracts.model.PeriodType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FinancialPeriodServiceTest {

    @Mock
    private ExpenseRepository expenseRepository;

    @Mock
    private RevenueRepository revenueRepository;

    @InjectMocks
    private FinancialPeriodService financialPeriodService;

    private final Long userId = 1L;
    private LocalDate today;

    @BeforeEach
    void setUp() {
        today = LocalDate.now();
    }

    @Nested
    class Expense {
        @ParameterizedTest
        @EnumSource(PeriodType.class)
        void shouldReturnExpensesInPeriod(PeriodType periodType) {
            LocalDate from = periodType.getStartDate(today);

            when(expenseRepository.sumExpensesByUserAndDateRange(userId, from, today)).thenReturn(Optional.of(BigDecimal.valueOf(50)));

            BigDecimal result = financialPeriodService.getExpensesSum(userId, periodType);

            assertThat(result).isEqualByComparingTo("50");
            verify(expenseRepository).sumExpensesByUserAndDateRange(userId, from, today);
        }

        @ParameterizedTest
        @EnumSource(PeriodType.class)
        void shouldReturnZeroWhenNoExpenses(PeriodType periodType) {
            LocalDate from = periodType.getStartDate(today);

            when(expenseRepository.sumExpensesByUserAndDateRange(userId, from, today)).thenReturn(Optional.empty());

            BigDecimal result = financialPeriodService.getExpensesSum(userId, periodType);

            assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @ParameterizedTest
        @EnumSource(PeriodType.class)
        void shouldReturnExpensesByCategory(PeriodType periodType) {
            LocalDate from = periodType.getStartDate(today);

            financialPeriodService.getExpensesInPeriodByCategory(userId, periodType, ExpenseCategory.FOOD);

            verify(expenseRepository).findAllByUserAssignedIdAndCreatedAtBetweenAndCategory(userId, from, today, ExpenseCategory.FOOD);
        }

        @ParameterizedTest
        @EnumSource(PeriodType.class)
        void shouldReturnAverageExpenseInPeriod(PeriodType periodType) {
            LocalDate from = periodType.getStartDate(today);

            when(expenseRepository.avgExpensesByUserAssignedIdAndPeriod(userId, from, today)).thenReturn(Optional.of(BigDecimal.valueOf(75)));

            BigDecimal result = financialPeriodService.getAverageExpense(userId, periodType);

            assertThat(result).isEqualByComparingTo("75");
            verify(expenseRepository).avgExpensesByUserAssignedIdAndPeriod(userId, from, today);
        }

        @ParameterizedTest
        @EnumSource(PeriodType.class)
        void shouldReturnZeroAverageExpenseWhenNoData(PeriodType periodType) {
            LocalDate from = periodType.getStartDate(today);

            when(expenseRepository.avgExpensesByUserAssignedIdAndPeriod(userId, from, today)).thenReturn(Optional.empty());

            BigDecimal result = financialPeriodService.getAverageExpense(userId, periodType);

            assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
        }
    }

    @Nested
    class Revenue {
        @ParameterizedTest
        @EnumSource(PeriodType.class)
        void shouldReturnRevenueInPeriod(PeriodType periodType) {
            LocalDate from = periodType.getStartDate(today);

            when(revenueRepository.sumRevenuesByUserAndDateRange(userId, from, today)).thenReturn(Optional.of(BigDecimal.valueOf(100)));

            BigDecimal result = financialPeriodService.getRevenueSum(userId, periodType);

            assertThat(result).isEqualByComparingTo("100");
            verify(revenueRepository).sumRevenuesByUserAndDateRange(userId, from, today);
        }

        @ParameterizedTest
        @EnumSource(PeriodType.class)
        void shouldReturnZeroWhenNoRevenue(PeriodType periodType) {
            LocalDate from = periodType.getStartDate(today);

            when(revenueRepository.sumRevenuesByUserAndDateRange(userId, from, today)).thenReturn(Optional.empty());

            BigDecimal result = financialPeriodService.getRevenueSum(userId, periodType);

            assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @ParameterizedTest
        @EnumSource(PeriodType.class)
        void shouldReturnRevenueByCategory(PeriodType periodType) {
            LocalDate from = periodType.getStartDate(today);

            financialPeriodService.getRevenuesInPeriodByCategory(userId, periodType, RevenueCategory.SALARY);

            verify(revenueRepository).findAllByUserAssignedIdAndCreatedAtBetweenAndCategory(userId, from, today, RevenueCategory.SALARY);
        }

        @ParameterizedTest
        @EnumSource(PeriodType.class)
        void shouldReturnAverageRevenueInPeriod(PeriodType periodType) {
            LocalDate from = periodType.getStartDate(today);

            when(revenueRepository.avgRevenuesByUserAssignedIdAndPeriod(userId, from, today)).thenReturn(Optional.of(BigDecimal.valueOf(200)));

            BigDecimal result = financialPeriodService.getAverageRevenue(userId, periodType);

            assertThat(result).isEqualByComparingTo("200");
            verify(revenueRepository).avgRevenuesByUserAssignedIdAndPeriod(userId, from, today);
        }

        @ParameterizedTest
        @EnumSource(PeriodType.class)
        void shouldReturnZeroAverageRevenueWhenNoData(PeriodType periodType) {
            LocalDate from = periodType.getStartDate(today);

            when(revenueRepository.avgRevenuesByUserAssignedIdAndPeriod(userId, from, today)).thenReturn(Optional.empty());

            BigDecimal result = financialPeriodService.getAverageRevenue(userId, periodType);

            assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
        }
    }
}