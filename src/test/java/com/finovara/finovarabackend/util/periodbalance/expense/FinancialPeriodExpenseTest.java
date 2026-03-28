package com.finovara.finovarabackend.util.periodbalance.expense;

import com.finovara.finovarabackend.expense.model.ExpenseCategory;
import com.finovara.finovarabackend.expense.repository.ExpenseRepository;
import com.finovara.finovarabackend.util.model.PeriodType;
import com.finovara.finovarabackend.util.service.periodbalance.FinancialPeriodService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class FinancialPeriodExpenseTest {

    @Mock
    private ExpenseRepository expenseRepository;

    @InjectMocks
    private FinancialPeriodService financialPeriodService;

    private final Long USER_ID = 1L;
    LocalDate today;

    @BeforeEach
    void setUp() {
        today = LocalDate.now();
    }

    @Test
    void shouldReturnSpentToday() {
        when(expenseRepository.sumExpensesByUserAndDateRange(USER_ID, today, today)).thenReturn(BigDecimal.valueOf(50));

        BigDecimal result = financialPeriodService.getSpent(USER_ID, PeriodType.DAILY);

        assertThat(result).isEqualByComparingTo("50");
        verify(expenseRepository).sumExpensesByUserAndDateRange(USER_ID, today, today);
    }

    @Test
    void shouldCalculateWeeklySpent() {
        LocalDate monday = today.with(DayOfWeek.MONDAY);

        when(expenseRepository.sumExpensesByUserAndDateRange(USER_ID, monday, today)).thenReturn(BigDecimal.valueOf(120));

        BigDecimal result = financialPeriodService.getSpent(USER_ID, PeriodType.WEEKLY);

        assertThat(result).isEqualByComparingTo("120");
    }

    @Test
    void shouldCalculateMonthlySpent() {
        LocalDate firstDayOfMonth = today.withDayOfMonth(1);

        when(expenseRepository.sumExpensesByUserAndDateRange(USER_ID, firstDayOfMonth, today)).thenReturn(BigDecimal.valueOf(300));

        BigDecimal result = financialPeriodService.getSpent(USER_ID, PeriodType.MONTHLY);

        assertThat(result).isEqualByComparingTo("300");
        verify(expenseRepository).sumExpensesByUserAndDateRange(USER_ID, firstDayOfMonth, today);
    }

    @Test
    void shouldReturnZeroWhenNoExpensesToday() {
        when(expenseRepository.sumExpensesByUserAndDateRange(USER_ID, today, today)).thenReturn(null);

        BigDecimal result = financialPeriodService.getSpent(USER_ID, PeriodType.DAILY);

        assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void shouldReturnZeroWhenNoWeeklyExpenses() {
        LocalDate monday = today.with(DayOfWeek.MONDAY);

        when(expenseRepository.sumExpensesByUserAndDateRange(USER_ID, monday, today)).thenReturn(null);

        BigDecimal result = financialPeriodService.getSpent(USER_ID, PeriodType.WEEKLY);

        assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void shouldReturnZeroWhenNoMonthlyExpenses() {
        LocalDate startOfMonth = today.withDayOfMonth(1);

        when(expenseRepository.sumExpensesByUserAndDateRange(USER_ID, startOfMonth, today)).thenReturn(null);

        BigDecimal result = financialPeriodService.getSpent(USER_ID, PeriodType.MONTHLY);

        assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void shouldFindExpensesInPeriod() {
        LocalDate monday = today.with(DayOfWeek.MONDAY);

        financialPeriodService.findExpensesInPeriod(USER_ID, PeriodType.WEEKLY);

        verify(expenseRepository).findAllByUserAssignedIdAndCreatedAtBetween(USER_ID, monday, today);
    }

    @Test
    void shouldFindExpensesInPeriodByCategory() {
        LocalDate firstDayOfMonth = today.withDayOfMonth(1);

        financialPeriodService.findExpensesInPeriodByCategory(USER_ID, PeriodType.MONTHLY, ExpenseCategory.FOOD);

        verify(expenseRepository).findAllByUserAssignedIdAndCreatedAtBetweenAndCategory(USER_ID, firstDayOfMonth, today, ExpenseCategory.FOOD);
    }

}
