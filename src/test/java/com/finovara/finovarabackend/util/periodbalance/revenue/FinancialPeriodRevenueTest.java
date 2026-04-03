package com.finovara.finovarabackend.util.periodbalance.revenue;

import com.finovara.finovarabackend.revenue.model.RevenueCategory;
import com.finovara.finovarabackend.revenue.repository.RevenueRepository;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FinancialPeriodRevenueTest {
    @Mock
    private RevenueRepository revenueRepository;

    @InjectMocks
    private FinancialPeriodService financialPeriodService;

    private final Long USER_ID = 1L;

    LocalDate today;

    @BeforeEach
    void setUp() {
        today = LocalDate.now();
    }
/*
    @Test
    void shouldReturnEarnedToday() {
        when(revenueRepository.sumRevenuesByUserAndDateRange(USER_ID, today, today)).thenReturn(BigDecimal.valueOf(50));

        BigDecimal result = financialPeriodService.getRevenueSum(USER_ID, PeriodType.DAILY);

        assertThat(result).isEqualByComparingTo("50");
    }

    @Test
    void shouldReturnEarnedWeekly() {
        LocalDate monday = today.with(DayOfWeek.MONDAY);

        when(revenueRepository.sumRevenuesByUserAndDateRange(USER_ID, monday, today)).thenReturn(BigDecimal.valueOf(75));

        BigDecimal result = financialPeriodService.getRevenueSum(USER_ID, PeriodType.WEEKLY);

        assertThat(result).isEqualByComparingTo("75");
        verify(revenueRepository).sumRevenuesByUserAndDateRange(USER_ID, monday, today);
    }

    @Test
    void shouldReturnEarnedMonthly() {
        LocalDate startOfMonth = today.withDayOfMonth(1);

        when(revenueRepository.sumRevenuesByUserAndDateRange(USER_ID, startOfMonth, today)).thenReturn(BigDecimal.valueOf(120));

        BigDecimal result = financialPeriodService.getRevenueSum(USER_ID, PeriodType.MONTHLY);

        assertThat(result).isEqualByComparingTo("120");
        verify(revenueRepository).sumRevenuesByUserAndDateRange(USER_ID, startOfMonth, today);
    }

    @Test
    void shouldReturnAllRevenuesInPeriod() {
        LocalDate monday = today.with(DayOfWeek.MONDAY);

        when(revenueRepository.findAllByUserAssignedIdAndCreatedAtBetween(USER_ID, monday, today)).thenReturn(List.of());
        financialPeriodService.getRevenuesInPeriod(USER_ID, PeriodType.WEEKLY);
        verify(revenueRepository).findAllByUserAssignedIdAndCreatedAtBetween(USER_ID, monday, today);
    }

    @Test
    void shouldReturnRevenuesByCategory() {
        LocalDate firstDayOfMonth = today.withDayOfMonth(1);

        financialPeriodService.getRevenuesInPeriodByCategory(USER_ID, PeriodType.MONTHLY, RevenueCategory.SALARY);

        verify(revenueRepository).findAllByUserAssignedIdAndCreatedAtBetweenAndCategory(USER_ID, firstDayOfMonth, today, RevenueCategory.SALARY);
    }

    @Test
    void shouldReturnZeroWhenNoRevenue() {
        when(revenueRepository.sumRevenuesByUserAndDateRange(USER_ID, today, today)).thenReturn(null);

        BigDecimal result = financialPeriodService.getRevenueSum(USER_ID, PeriodType.DAILY);

        assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
    }*/
}