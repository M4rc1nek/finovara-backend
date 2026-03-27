package com.finovara.finovarabackend.report.finances.service.sum;

import com.finovara.finovarabackend.report.dto.ReportDto;
import com.finovara.finovarabackend.util.model.PeriodType;
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
class ReportsSumExpenseTest {
    @Mock
    private FinancialPeriodService financialPeriodService;

    @InjectMocks
    private ReportSumService reportSumService;

    private final Long USER_ID  = 1L;

    @Test
    void shouldReturnDailySum() {
        BigDecimal expected = BigDecimal.valueOf(100);

        when(financialPeriodService.getSpent(USER_ID, PeriodType.DAILY)).thenReturn(expected);

        ReportDto result = reportSumService.sumExpense(USER_ID, PeriodType.DAILY);

        assertEquals(PeriodType.DAILY, result.periodType());
        assertEquals(expected, result.amount());
        verify(financialPeriodService).getSpent(USER_ID, PeriodType.DAILY);
    }

    @Test
    void shouldReturnWeeklySum() {
        BigDecimal expected = BigDecimal.valueOf(200);

        when(financialPeriodService.getSpent(USER_ID, PeriodType.WEEKLY)).thenReturn(expected);

        ReportDto result = reportSumService.sumExpense(USER_ID, PeriodType.WEEKLY);

        assertEquals(PeriodType.WEEKLY, result.periodType());
        assertEquals(expected, result.amount());
        verify(financialPeriodService).getSpent(USER_ID, PeriodType.WEEKLY);
    }

    @Test
    void shouldReturnMonthlySum() {
        BigDecimal expected = BigDecimal.valueOf(300);

        when(financialPeriodService.getSpent(USER_ID, PeriodType.MONTHLY)).thenReturn(expected);

        ReportDto result = reportSumService.sumExpense(USER_ID, PeriodType.MONTHLY);

        assertEquals(PeriodType.MONTHLY, result.periodType());
        assertEquals(expected, result.amount());
        verify(financialPeriodService).getSpent(USER_ID, PeriodType.MONTHLY);
    }
}