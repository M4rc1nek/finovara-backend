package com.finovara.finovarabackend.report.finances.service.chart;

import com.finovara.finovarabackend.expense.repository.ExpenseRepository;
import com.finovara.finovarabackend.report.finances.dto.ReportMonthlyChartDTO;
import com.finovara.finovarabackend.report.finances.service.ReportsService;
import com.finovara.finovarabackend.revenue.repository.RevenueRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportsMonthlyChartTest {

    @Mock
    private RevenueRepository revenueRepository;

    @Mock
    private ExpenseRepository expenseRepository;

    @InjectMocks
    private ReportsService reportsService;

    private final Long USER_ID = 1L;

    @Test
    void shouldGenerateMonthlyChartWithMockedData() {

        LocalDate today = LocalDate.now();
        LocalDate day1 = today.withDayOfMonth(1);
        LocalDate day2 = today.withDayOfMonth(2);

        when(revenueRepository.sumRevenueForDay(USER_ID, day1)).thenReturn(BigDecimal.valueOf(100));
        when(expenseRepository.sumExpenseForDay(USER_ID, day1)).thenReturn(BigDecimal.valueOf(50));

        when(revenueRepository.sumRevenueForDay(USER_ID, day2)).thenReturn(BigDecimal.valueOf(200));
        when(expenseRepository.sumExpenseForDay(USER_ID, day2)).thenReturn(BigDecimal.valueOf(70));

        List<ReportMonthlyChartDTO> chartData = reportsService.getMonthlyChart(USER_ID);

        ReportMonthlyChartDTO day1Chart = chartData.getFirst();
        assertThat(day1Chart.day()).isEqualTo(1);
        assertThat(day1Chart.income()).isEqualByComparingTo(BigDecimal.valueOf(100));
        assertThat(day1Chart.expense()).isEqualByComparingTo(BigDecimal.valueOf(50));

        ReportMonthlyChartDTO day2Chart = chartData.get(1);
        assertThat(day2Chart.day()).isEqualTo(2);
        assertThat(day2Chart.income()).isEqualByComparingTo(BigDecimal.valueOf(200));
        assertThat(day2Chart.expense()).isEqualByComparingTo(BigDecimal.valueOf(70));
    }
    @Test
    void shouldHandleNullIncomeAndExpenseForDay() {

        LocalDate day = LocalDate.now();

        BigDecimal incomeForDay = revenueRepository.sumRevenueForDay(USER_ID, day);
        BigDecimal expenseForDay = expenseRepository.sumExpenseForDay(USER_ID, day);

        if (incomeForDay == null) incomeForDay = BigDecimal.ZERO;
        if (expenseForDay == null) expenseForDay = BigDecimal.ZERO;

        assertThat(incomeForDay).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(expenseForDay).isEqualByComparingTo(BigDecimal.ZERO);
    }
}