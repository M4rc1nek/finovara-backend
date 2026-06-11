package com.finovara.corebackend.report.finances.sum.service;

import com.finovara.corebackend.report.dto.ReportDto;
import com.finovara.contracts.model.PeriodType;
import com.finovara.corebackend.util.periodbalance.FinancialPeriodService;
import org.assertj.core.api.AssertionsForClassTypes;
import org.junit.jupiter.api.Nested;
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
class ReportSummaryServiceTest {

    @Mock
    private FinancialPeriodService financialPeriodService;

    @InjectMocks
    private ReportSummaryService reportSummaryService;

    @Nested
    class SumRevenue {
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

    @Nested
    class sumExpense {
        @ParameterizedTest
        @EnumSource(PeriodType.class)
        void shouldSumExpenseInPeriod(PeriodType periodType) {
            Long userId = 1L;
            BigDecimal amount = BigDecimal.valueOf(100);

            when(financialPeriodService.getExpensesSum(userId, periodType)).thenReturn(amount);

            ReportDto result = reportSummaryService.sumExpense(userId, periodType);

            AssertionsForClassTypes.assertThat(result.amount()).isEqualByComparingTo("100");
            AssertionsForClassTypes.assertThat(result.periodType()).isEqualTo(periodType);
            verify(financialPeriodService).getExpensesSum(userId, periodType);
            verifyNoMoreInteractions(financialPeriodService);
        }

        @Test
        void shouldReturnZeroWhenNoData() {
            Long userId = 1L;

            when(financialPeriodService.getExpensesSum(userId, PeriodType.DAILY)).thenReturn(BigDecimal.ZERO);

            ReportDto result = reportSummaryService.sumExpense(userId, PeriodType.DAILY);

            AssertionsForClassTypes.assertThat(result.amount()).isEqualByComparingTo("0");
            verifyNoMoreInteractions(financialPeriodService);
        }
    }
}