package com.finovara.financeservice.internal.digest.report.email.service;

import com.finovara.contracts.model.PeriodType;
import com.finovara.contracts.model.transaction.ExpenseCategory;
import com.finovara.financeservice.expense.model.Expense;
import com.finovara.financeservice.expense.repository.ExpenseRepository;
import com.finovara.financeservice.internal.digest.report.email.dto.ExpenseSummary;
import com.finovara.financeservice.limit.model.Limit;
import com.finovara.financeservice.limit.repository.LimitRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExpenseDigestServiceTest {

    private static final Long USER_ID = 1L;
    private static final LocalDate FROM = LocalDate.of(2026, 8, 10);
    private static final LocalDate TO = LocalDate.of(2026, 8, 16);

    @Mock
    private ExpenseRepository expenseRepository;

    @Mock
    private LimitRepository limitRepository;

    private ExpenseDigestService service;

    @BeforeEach
    void setUp() {
        service = new ExpenseDigestService(expenseRepository, limitRepository);
        when(limitRepository.findGeneralLimit(USER_ID, PeriodType.MONTHLY)).thenReturn(Optional.empty());
    }

    private Expense expense(BigDecimal amount, ExpenseCategory category, LocalDate createdAt) {
        Expense expense = mock(Expense.class);
        when(expense.getAmount()).thenReturn(amount);
        when(expense.getCategory()).thenReturn(category);
        when(expense.getCreatedAt()).thenReturn(createdAt);
        return expense;
    }

    @Nested
    class CalculateSummary {

        @Test
        void shouldReturnCompleteSummaryWhenDataExists() {
            Expense highestExpense = expense(new BigDecimal("150"), ExpenseCategory.FOOD, LocalDate.of(2026, 8, 12));

            when(expenseRepository.sumExpensesByUserAndDateRange(USER_ID, FROM, TO)).thenReturn(Optional.of(new BigDecimal("400")));
            when(expenseRepository.findTopExpenseCategoriesByUserIdAndPeriod(eq(USER_ID), eq(FROM), eq(TO), eq(PageRequest.of(0, 1)))).thenReturn(List.of(ExpenseCategory.FOOD));
            when(expenseRepository.findTopExpensesByUserIdAndPeriod(eq(USER_ID), eq(FROM), eq(TO), eq(PageRequest.of(0, 1)))).thenReturn(List.of(highestExpense));
            when(expenseRepository.findAllByUserIdAndCreatedAtBetween(USER_ID, FROM, TO)).thenReturn(List.of(highestExpense));

            ExpenseSummary result = service.calculateSummary(USER_ID, FROM, TO);

            assertEquals(new BigDecimal("400"), result.sum());
            assertEquals("FOOD", result.topCategory());
            assertEquals(new BigDecimal("150"), result.highestAmount());
            assertEquals("FOOD", result.highestCategory());
            assertEquals(LocalDate.of(2026, 8, 12), result.highestDate());
        }

        @Test
        void shouldReturnZeroSumWhenNoExpensesInRange() {
            when(expenseRepository.sumExpensesByUserAndDateRange(USER_ID, FROM, TO)).thenReturn(Optional.empty());
            when(expenseRepository.findTopExpenseCategoriesByUserIdAndPeriod(eq(USER_ID), eq(FROM), eq(TO), eq(PageRequest.of(0, 1)))).thenReturn(List.of());
            when(expenseRepository.findTopExpensesByUserIdAndPeriod(eq(USER_ID), eq(FROM), eq(TO), eq(PageRequest.of(0, 1)))).thenReturn(List.of());
            when(expenseRepository.findAllByUserIdAndCreatedAtBetween(USER_ID, FROM, TO)).thenReturn(List.of());

            ExpenseSummary result = service.calculateSummary(USER_ID, FROM, TO);

            assertEquals(BigDecimal.ZERO, result.sum());
        }

        @Test
        void shouldReturnNullTopCategoryWhenNoCategoriesFound() {
            when(expenseRepository.sumExpensesByUserAndDateRange(USER_ID, FROM, TO)).thenReturn(Optional.of(BigDecimal.ZERO));
            when(expenseRepository.findTopExpenseCategoriesByUserIdAndPeriod(eq(USER_ID), eq(FROM), eq(TO), eq(PageRequest.of(0, 1)))).thenReturn(List.of());
            when(expenseRepository.findTopExpensesByUserIdAndPeriod(eq(USER_ID), eq(FROM), eq(TO), eq(PageRequest.of(0, 1)))).thenReturn(List.of());
            when(expenseRepository.findAllByUserIdAndCreatedAtBetween(USER_ID, FROM, TO)).thenReturn(List.of());

            ExpenseSummary result = service.calculateSummary(USER_ID, FROM, TO);

            assertNull(result.topCategory());
        }

        @Test
        void shouldReturnZeroHighestAmountAndNullFieldsWhenNoExpenseFound() {
            when(expenseRepository.sumExpensesByUserAndDateRange(USER_ID, FROM, TO)).thenReturn(Optional.of(BigDecimal.ZERO));
            when(expenseRepository.findTopExpenseCategoriesByUserIdAndPeriod(eq(USER_ID), eq(FROM), eq(TO), eq(PageRequest.of(0, 1)))).thenReturn(List.of());
            when(expenseRepository.findTopExpensesByUserIdAndPeriod(eq(USER_ID), eq(FROM), eq(TO), eq(PageRequest.of(0, 1)))).thenReturn(List.of());
            when(expenseRepository.findAllByUserIdAndCreatedAtBetween(USER_ID, FROM, TO)).thenReturn(List.of());

            ExpenseSummary result = service.calculateSummary(USER_ID, FROM, TO);

            assertEquals(BigDecimal.ZERO, result.highestAmount());
            assertNull(result.highestCategory());
            assertNull(result.highestDate());
        }

        @Test
        void shouldReturnFullDaysWithoutExpenseWhenNoExpensesFound() {
            when(expenseRepository.sumExpensesByUserAndDateRange(USER_ID, FROM, TO)).thenReturn(Optional.of(BigDecimal.ZERO));
            when(expenseRepository.findTopExpenseCategoriesByUserIdAndPeriod(eq(USER_ID), eq(FROM), eq(TO), eq(PageRequest.of(0, 1)))).thenReturn(List.of());
            when(expenseRepository.findTopExpensesByUserIdAndPeriod(eq(USER_ID), eq(FROM), eq(TO), eq(PageRequest.of(0, 1)))).thenReturn(List.of());
            when(expenseRepository.findAllByUserIdAndCreatedAtBetween(USER_ID, FROM, TO)).thenReturn(List.of());

            ExpenseSummary result = service.calculateSummary(USER_ID, FROM, TO);

            assertEquals(7, result.daysWithoutExpense());
        }

        @Test
        void shouldReturnZeroRemainingBudgetPercentageWhenNoLimitFound() {
            when(expenseRepository.sumExpensesByUserAndDateRange(USER_ID, FROM, TO)).thenReturn(Optional.of(BigDecimal.ZERO));
            when(expenseRepository.findTopExpenseCategoriesByUserIdAndPeriod(eq(USER_ID), eq(FROM), eq(TO), eq(PageRequest.of(0, 1)))).thenReturn(List.of());
            when(expenseRepository.findTopExpensesByUserIdAndPeriod(eq(USER_ID), eq(FROM), eq(TO), eq(PageRequest.of(0, 1)))).thenReturn(List.of());
            when(expenseRepository.findAllByUserIdAndCreatedAtBetween(USER_ID, FROM, TO)).thenReturn(List.of());

            ExpenseSummary result = service.calculateSummary(USER_ID, FROM, TO);

            assertEquals(0, BigDecimal.ZERO.compareTo(result.remainingBudgetPercentage()));
        }

        @Test
        void shouldCalculateRemainingBudgetPercentageWhenLimitExists() {
            Limit limit = mock(Limit.class);
            when(limit.getAmount()).thenReturn(new BigDecimal("200"));

            when(limitRepository.findGeneralLimit(USER_ID, PeriodType.MONTHLY)).thenReturn(Optional.of(limit));
            when(expenseRepository.sumExpensesByUserAndDateRange(eq(USER_ID), any(LocalDate.class), any(LocalDate.class))).thenReturn(Optional.of(new BigDecimal("50")));
            when(expenseRepository.findTopExpenseCategoriesByUserIdAndPeriod(eq(USER_ID), eq(FROM), eq(TO), eq(PageRequest.of(0, 1)))).thenReturn(List.of());
            when(expenseRepository.findTopExpensesByUserIdAndPeriod(eq(USER_ID), eq(FROM), eq(TO), eq(PageRequest.of(0, 1)))).thenReturn(List.of());
            when(expenseRepository.findAllByUserIdAndCreatedAtBetween(USER_ID, FROM, TO)).thenReturn(List.of());

            ExpenseSummary result = service.calculateSummary(USER_ID, FROM, TO);

            assertEquals(0, new BigDecimal("75").compareTo(result.remainingBudgetPercentage()));
        }

        @Test
        void shouldReturnZeroRemainingBudgetWhenSpentExceedsLimit() {
            Limit limit = mock(Limit.class);
            when(limit.getAmount()).thenReturn(new BigDecimal("100"));

            when(limitRepository.findGeneralLimit(USER_ID, PeriodType.MONTHLY)).thenReturn(Optional.of(limit));
            when(expenseRepository.sumExpensesByUserAndDateRange(eq(USER_ID), any(LocalDate.class), any(LocalDate.class))).thenReturn(Optional.of(new BigDecimal("300")));
            when(expenseRepository.findTopExpenseCategoriesByUserIdAndPeriod(eq(USER_ID), eq(FROM), eq(TO), eq(PageRequest.of(0, 1)))).thenReturn(List.of());
            when(expenseRepository.findTopExpensesByUserIdAndPeriod(eq(USER_ID), eq(FROM), eq(TO), eq(PageRequest.of(0, 1)))).thenReturn(List.of());
            when(expenseRepository.findAllByUserIdAndCreatedAtBetween(USER_ID, FROM, TO)).thenReturn(List.of());

            ExpenseSummary result = service.calculateSummary(USER_ID, FROM, TO);

            assertEquals(0, BigDecimal.ZERO.compareTo(result.remainingBudgetPercentage()));
        }

        @Test
        void shouldQueryLimitRepositoryWithMonthlyPeriodType() {
            when(expenseRepository.sumExpensesByUserAndDateRange(USER_ID, FROM, TO)).thenReturn(Optional.of(BigDecimal.ZERO));
            when(expenseRepository.findTopExpenseCategoriesByUserIdAndPeriod(eq(USER_ID), eq(FROM), eq(TO), eq(PageRequest.of(0, 1)))).thenReturn(List.of());
            when(expenseRepository.findTopExpensesByUserIdAndPeriod(eq(USER_ID), eq(FROM), eq(TO), eq(PageRequest.of(0, 1)))).thenReturn(List.of());
            when(expenseRepository.findAllByUserIdAndCreatedAtBetween(USER_ID, FROM, TO)).thenReturn(List.of());

            service.calculateSummary(USER_ID, FROM, TO);

            assertEquals(Optional.empty(), limitRepository.findGeneralLimit(USER_ID, PeriodType.MONTHLY));
        }
    }
}