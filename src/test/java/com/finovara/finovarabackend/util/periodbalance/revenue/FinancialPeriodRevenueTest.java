package com.finovara.finovarabackend.util.periodbalance;

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
import java.util.List;

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

    private final Long USER_ID = 1L;


    @Test
    void shouldReturnEarnedWeekly() {
        LocalDate today = LocalDate.now();
        LocalDate monday = today.with(DayOfWeek.MONDAY);

        when(revenueRepository.sumRevenuesByUserAndDateRange(USER_ID, monday, today)).thenReturn(BigDecimal.valueOf(75));

        BigDecimal result = financialPeriodService.getEarned(USER_ID, PeriodType.WEEKLY);

        assertThat(result).isEqualByComparingTo("75");
    }

    @Test
    void shouldReturnAllRevenuesInPeriod() {
        LocalDate today = LocalDate.now();
        LocalDate monday = today.with(DayOfWeek.MONDAY);

        when(revenueRepository.findAllByUserAssignedIdAndCreatedAtBetween(USER_ID, monday, today)).thenReturn(List.of());
        financialPeriodService.findRevenuesInPeriod(USER_ID, PeriodType.WEEKLY);
        verify(revenueRepository).findAllByUserAssignedIdAndCreatedAtBetween(USER_ID, monday, today);
    }

    @Test
    void shouldReturnAllExpensesInPeriod() {
        LocalDate today = LocalDate.now();
        LocalDate startOfMonth = today.withDayOfMonth(1);

        when(expenseRepository.findAllByUserAssignedIdAndCreatedAtBetween(USER_ID, startOfMonth, today)).thenReturn(List.of());
        financialPeriodService.findExpensesInPeriod(USER_ID, PeriodType.MONTHLY);
        verify(expenseRepository).findAllByUserAssignedIdAndCreatedAtBetween(USER_ID, startOfMonth, today);

    }
}