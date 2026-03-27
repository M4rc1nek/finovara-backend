package com.finovara.finovarabackend.util.time;

import com.finovara.finovarabackend.expense.repository.ExpenseRepository;
import com.finovara.finovarabackend.revenue.repository.RevenueRepository;
import com.finovara.finovarabackend.util.model.PeriodType;
import com.finovara.finovarabackend.util.service.periodbalance.FinancialPeriodService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FinancialPeriodServiceTest {

    @Mock
    private ExpenseRepository expenseRepository;

    @Mock
    private RevenueRepository revenueRepository;

    @InjectMocks
    private FinancialPeriodService financialPeriodService;

    private final Long USER_ID = 1L;

    @Test
    void shouldReturnSpentToday() {
        LocalDate today = LocalDate.now();

        when(expenseRepository.sumExpensesByUserAndDateRange(USER_ID, today, today))
                .thenReturn(BigDecimal.valueOf(50));

        BigDecimal result = financialPeriodService.getSpent(USER_ID, PeriodType.DAILY);

        assertThat(result).isEqualByComparingTo("50");
    }

    @Test
    void shouldReturnZeroWhenNoExpensesToday() {
        LocalDate today = LocalDate.now();

        when(expenseRepository.sumExpensesByUserAndDateRange(USER_ID, today, today))
                .thenReturn(null);

        BigDecimal result = financialPeriodService.getSpent(USER_ID, PeriodType.DAILY);

        assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void shouldCalculateWeeklySpent() {
        LocalDate today = LocalDate.now();
        LocalDate monday = today.with(DayOfWeek.MONDAY);

        when(expenseRepository.sumExpensesByUserAndDateRange(USER_ID, monday, today))
                .thenReturn(BigDecimal.valueOf(120));

        BigDecimal result = financialPeriodService.getSpent(USER_ID, PeriodType.WEEKLY);

        assertThat(result).isEqualByComparingTo("120");
    }

    @Test
    void shouldCalculateMonthlySpent() {
        LocalDate today = LocalDate.now();
        LocalDate firstDayOfMonth = today.withDayOfMonth(1);

        when(expenseRepository.sumExpensesByUserAndDateRange(USER_ID, firstDayOfMonth, today))
                .thenReturn(BigDecimal.valueOf(300));

        BigDecimal result = financialPeriodService.getSpent(USER_ID, PeriodType.MONTHLY);

        assertThat(result).isEqualByComparingTo("300");
    }

    @Test
    void shouldCallRepositoryWithCorrectDatesForWeekly() {
        financialPeriodService.getSpent(USER_ID, PeriodType.WEEKLY);

        LocalDate today = LocalDate.now();
        LocalDate monday = today.with(DayOfWeek.MONDAY);

        verify(expenseRepository)
                .sumExpensesByUserAndDateRange(USER_ID, monday, today);
    }

    @Test
    void shouldReturnEarnedWeekly() {
        LocalDate today = LocalDate.now();
        LocalDate monday = today.with(DayOfWeek.MONDAY);

        when(revenueRepository.sumRevenuesByUserAndDateRange(USER_ID, monday, today))
                .thenReturn(BigDecimal.valueOf(75));

        BigDecimal result = financialPeriodService.getEarned(USER_ID, PeriodType.WEEKLY);

        assertThat(result).isEqualByComparingTo("75");
    }
}