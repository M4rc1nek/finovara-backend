package com.finovara.finovarabackend.report.finances.service.sum;

import com.finovara.finovarabackend.report.dto.ReportDto;
import com.finovara.finovarabackend.report.finances.sum.service.ReportSummaryService;
import com.finovara.finovarabackend.util.model.PeriodType;
import com.finovara.finovarabackend.util.periodbalance.FinancialPeriodService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportsSumRevenueTest {
    @Mock
    private FinancialPeriodService financialPeriodService;

    @InjectMocks
    private ReportSummaryService reportSummaryService;


    @ParameterizedTest
    @EnumSource(PeriodType.class)
    void shouldSumRevenueInPeriod(PeriodType periodType) {
        Long userId = 1L;
        BigDecimal amount = BigDecimal.valueOf(100);

        when(financialPeriodService.getRevenueSum(userId, periodType)).thenReturn(amount);

        ReportDto result = reportSummaryService.sumRevenue(userId, periodType);

        assertThat(result.amount()).isEqualByComparingTo("100");
        assertThat(result.periodType()).isEqualTo(periodType);
        verify(financialPeriodService).getRevenueSum(userId, periodType);
        verifyNoMoreInteractions(financialPeriodService);
    }


    @Test
    void shouldReturnZeroWhenNoData() {
        Long userId = 1L;

        when(financialPeriodService.getRevenueSum(userId, PeriodType.DAILY)).thenReturn(BigDecimal.ZERO);

        ReportDto result = reportSummaryService.sumRevenue(userId, PeriodType.DAILY);

        assertThat(result.amount()).isEqualByComparingTo("0");
        verifyNoMoreInteractions(financialPeriodService);
    }

}