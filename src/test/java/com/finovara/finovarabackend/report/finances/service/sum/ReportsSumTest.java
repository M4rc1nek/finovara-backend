package com.finovara.finovarabackend.report.finances.service.sum;

import com.finovara.finovarabackend.report.finances.sum.dto.ReportSumDto;
import com.finovara.finovarabackend.report.finances.sum.model.ReportSumType;
import com.finovara.finovarabackend.report.finances.sum.sevice.ReportSumService;
import com.finovara.finovarabackend.util.service.time.SpentInPeriodService;
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
class ReportsSumTest {
    @Mock
    private SpentInPeriodService spentInPeriodService;

    @InjectMocks
    private ReportSumService reportSumService;

    private final Long USER_ID  = 1L;

    @Test
    void shouldReturnDailySum() {
        BigDecimal expected = BigDecimal.valueOf(100);

        when(spentInPeriodService.getSummedSpentToday(USER_ID)).thenReturn(expected);

        ReportSumDto result = reportSumService.sumRevenueAndExpense(USER_ID, ReportSumType.DAILY);

        assertEquals(ReportSumType.DAILY, result.reportSumType());
        assertEquals(expected, result.amount());
        verify(spentInPeriodService).getSummedSpentToday(USER_ID);
    }

    @Test
    void shouldReturnWeeklySum() {
        BigDecimal expected = BigDecimal.valueOf(200);

        when(spentInPeriodService.getSummedSpentWeekly(USER_ID)).thenReturn(expected);

        ReportSumDto result = reportSumService.sumRevenueAndExpense(USER_ID, ReportSumType.WEEKLY);

        assertEquals(ReportSumType.WEEKLY, result.reportSumType());
        assertEquals(expected, result.amount());
        verify(spentInPeriodService).getSummedSpentWeekly(USER_ID);
    }

    @Test
    void shouldReturnMonthlySum() {
        BigDecimal expected = BigDecimal.valueOf(300);

        when(spentInPeriodService.getSummedSpentMonthly(USER_ID)).thenReturn(expected);

        ReportSumDto result = reportSumService.sumRevenueAndExpense(USER_ID, ReportSumType.MONTHLY);

        assertEquals(ReportSumType.MONTHLY, result.reportSumType());
        assertEquals(expected, result.amount());
        verify(spentInPeriodService).getSummedSpentMonthly(USER_ID);
    }
}