package com.finovara.finovarabackend.report.finances.service.average;

import com.finovara.finovarabackend.revenue.model.Revenue;
import com.finovara.finovarabackend.revenue.repository.RevenueRepository;
import com.finovara.finovarabackend.report.dto.ReportDto;
import com.finovara.finovarabackend.report.finances.average.service.ReportAverageService;
import com.finovara.finovarabackend.report.model.ReportPeriodType;
import com.finovara.finovarabackend.util.service.periodbalance.FinancialPeriodService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportsAverageRevenueTest {

    @Mock
    private FinancialPeriodService financialPeriodService;

    @Mock
    private RevenueRepository revenueRepository;

    @InjectMocks
    private ReportAverageService reportAverageService;

    @Test
    void shouldReturnCorrectDailyAverageRevenue() {
        Long userId = 1L;
        List<Revenue> revenues = List.of(new Revenue(), new Revenue());

        when(revenueRepository.findAllByUserAssignedId(userId)).thenReturn(revenues);
        when(financialPeriodService.getSummedRevenuesToday(userId)).thenReturn(BigDecimal.valueOf(200));

        ReportDto result = reportAverageService.calculateAverageRevenue(userId, ReportPeriodType.DAILY);

        assertThat(result.amount()).isEqualByComparingTo(BigDecimal.valueOf(100));

        assertThat(result.reportPeriodType()).isEqualTo(ReportPeriodType.DAILY);
    }

    @Test
    void shouldReturnCorrectWeeklyAverageRevenue() {
        Long userId = 1L;
        List<Revenue> revenues = List.of(new Revenue(), new Revenue(), new Revenue());

        when(revenueRepository.findAllByUserAssignedId(userId)).thenReturn(revenues);
        when(financialPeriodService.getSummedRevenuesWeekly(userId)).thenReturn(BigDecimal.valueOf(300));

        ReportDto result = reportAverageService.calculateAverageRevenue(userId, ReportPeriodType.WEEKLY);

        assertThat(result.amount()).isEqualByComparingTo(BigDecimal.valueOf(100));

        assertThat(result.reportPeriodType()).isEqualTo(ReportPeriodType.WEEKLY);
    }

    @Test
    void shouldReturnCorrectMonthlyAverageRevenue() {
        Long userId = 1L;
        List<Revenue> revenues = List.of(new Revenue());

        when(revenueRepository.findAllByUserAssignedId(userId)).thenReturn(revenues);
        when(financialPeriodService.getSummedRevenuesMonthly(userId)).thenReturn(BigDecimal.valueOf(500));

        ReportDto result = reportAverageService.calculateAverageRevenue(userId, ReportPeriodType.MONTHLY);

        assertThat(result.amount()).isEqualByComparingTo(BigDecimal.valueOf(500));

        assertThat(result.reportPeriodType()).isEqualTo(ReportPeriodType.MONTHLY);
    }
}