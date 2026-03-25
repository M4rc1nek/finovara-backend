package com.finovara.finovarabackend.util.time;

import com.finovara.finovarabackend.expense.repository.ExpenseRepository;
import com.finovara.finovarabackend.util.service.time.SpentInPeriodService;
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
class SpentInPeriodServiceTest {

    @Mock
    private ExpenseRepository expenseRepository;

    @InjectMocks
    private SpentInPeriodService spentInPeriodService;

    private final Long USER_ID = 1L;
    @Test
    void shouldReturnTodayDate() {
        LocalDate today = LocalDate.now();
        LocalDate result = spentInPeriodService.today();

        assertThat(result).isEqualTo(today);
    }

    @Test
    void shouldReturnSpentToday() {
        LocalDate today = LocalDate.now();

        when(expenseRepository.sumExpensesByUserAndDateRange(USER_ID, today, today))
                .thenReturn(BigDecimal.valueOf(50));

        BigDecimal result = spentInPeriodService.getSpentToday(USER_ID);

        assertThat(result).isEqualByComparingTo("50");
    }

    @Test
    void shouldReturnZeroWhenNoExpensesToday() {
        LocalDate today = LocalDate.now();

        when(expenseRepository.sumExpensesByUserAndDateRange(USER_ID, today, today))
                .thenReturn(null);

        BigDecimal result = spentInPeriodService.getSpentToday(USER_ID);

        assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void shouldCalculateWeeklySpent() {
        LocalDate today = LocalDate.now();
        LocalDate monday = today.with(DayOfWeek.MONDAY);

        when(expenseRepository.sumExpensesByUserAndDateRange(USER_ID, monday, today))
                .thenReturn(BigDecimal.valueOf(120));

        BigDecimal result = spentInPeriodService.getSpentWeekly(USER_ID);

        assertThat(result).isEqualByComparingTo("120");
    }

    @Test
    void shouldCalculateMonthlySpent() {
        LocalDate today = LocalDate.now();
        LocalDate firstDayOfMonth = today.withDayOfMonth(1);

        when(expenseRepository.sumExpensesByUserAndDateRange(USER_ID, firstDayOfMonth, today))
                .thenReturn(BigDecimal.valueOf(300));

        BigDecimal result = spentInPeriodService.getSpentMonthly(USER_ID);

        assertThat(result).isEqualByComparingTo("300");
    }

    @Test
    void shouldCallRepositoryWithCorrectDatesForWeekly() {
        spentInPeriodService.getSpentWeekly(USER_ID);

        LocalDate today = LocalDate.now();
        LocalDate monday = today.with(DayOfWeek.MONDAY);

        verify(expenseRepository)
                .sumExpensesByUserAndDateRange(USER_ID, monday, today);
    }
}