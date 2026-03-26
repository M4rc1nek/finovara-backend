package com.finovara.finovarabackend.report.finances.service.sum;

import com.finovara.finovarabackend.report.dto.ReportDto;
import com.finovara.finovarabackend.report.model.ReportPeriodType;
import com.finovara.finovarabackend.report.finances.sum.sevice.ReportSumService;
import com.finovara.finovarabackend.util.service.periodbalance.FinancialPeriodService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportsSumRevenueTest {
    @Mock
    private FinancialPeriodService financialPeriodService;

    @InjectMocks
    private ReportSumService reportSumService;

    private final Long USER_ID  = 1L;

    @Test
    void shouldReturnDailySum() {
        BigDecimal expected = BigDecimal.valueOf(250);

        when(financialPeriodService.getSummedRevenuesToday(USER_ID)).thenReturn(expected);

        ReportDto result = reportSumService.sumRevenue(USER_ID, ReportPeriodType.DAILY);

        assertEquals(ReportPeriodType.DAILY, result.reportPeriodType());
        assertEquals(expected, result.amount());
        verify(financialPeriodService).getSummedRevenuesToday(USER_ID);
    }

    @Test
    void shouldReturnWeeklySum() {
        BigDecimal expected = BigDecimal.valueOf(300);

        when(financialPeriodService.getSummedRevenuesWeekly(USER_ID)).thenReturn(expected);

        ReportDto result = reportSumService.sumRevenue(USER_ID, ReportPeriodType.WEEKLY);

        assertEquals(ReportPeriodType.WEEKLY, result.reportPeriodType());
        assertEquals(expected, result.amount());
        verify(financialPeriodService).getSummedRevenuesWeekly(USER_ID);
    }

    @Test
    void shouldReturnMonthlySum() {
        BigDecimal expected = BigDecimal.valueOf(500);

        when(financialPeriodService.getSummedRevenuesMonthly(USER_ID)).thenReturn(expected);

        ReportDto result = reportSumService.sumRevenue(USER_ID, ReportPeriodType.MONTHLY);

        assertEquals(ReportPeriodType.MONTHLY, result.reportPeriodType());
        assertEquals(expected, result.amount());
        verify(financialPeriodService).getSummedRevenuesMonthly(USER_ID);
    }
}