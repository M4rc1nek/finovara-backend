package com.finovara.reportservice.report.finances.sum.service;

import com.finovara.contracts.model.PeriodType;
import com.finovara.reportservice.feignclient.CoreBackendReportClient;
import com.finovara.reportservice.report.dto.ReportDto;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportSummaryServiceTest {

    private static final Long USER_ID = 1L;

    @Mock
    private CoreBackendReportClient reportClient;

    @InjectMocks
    private ReportSummaryService reportSummaryService;

    @Nested
    class SumExpense {

        @ParameterizedTest
        @EnumSource(PeriodType.class)
        void shouldDelegateToClientAndReturnDto(PeriodType periodType) {
            LocalDate to = LocalDate.now();
            LocalDate from = periodType.getStartDate(to);

            when(reportClient.sumExpenses(USER_ID, from, to)).thenReturn(BigDecimal.valueOf(100));

            ReportDto result = reportSummaryService.sumExpense(USER_ID, periodType);

            assertThat(result.amount()).isEqualByComparingTo("100");
            assertThat(result.periodType()).isEqualTo(periodType);
            verify(reportClient).sumExpenses(USER_ID, from, to);
        }

        @Test
        void shouldReturnZeroAmountWhenClientReturnsZero() {
            LocalDate to = LocalDate.now();
            LocalDate from = PeriodType.DAILY.getStartDate(to);

            when(reportClient.sumExpenses(USER_ID, from, to)).thenReturn(BigDecimal.ZERO);

            ReportDto result = reportSummaryService.sumExpense(USER_ID, PeriodType.DAILY);

            assertThat(result.amount()).isEqualByComparingTo("0");
        }
    }

    @Nested
    class SumRevenue {

        @ParameterizedTest
        @EnumSource(PeriodType.class)
        void shouldDelegateToClientAndReturnDto(PeriodType periodType) {
            LocalDate to = LocalDate.now();
            LocalDate from = periodType.getStartDate(to);

            when(reportClient.sumRevenues(USER_ID, from, to)).thenReturn(BigDecimal.valueOf(200));

            ReportDto result = reportSummaryService.sumRevenue(USER_ID, periodType);

            assertThat(result.amount()).isEqualByComparingTo("200");
            assertThat(result.periodType()).isEqualTo(periodType);
            verify(reportClient).sumRevenues(USER_ID, from, to);
        }

        @Test
        void shouldReturnZeroAmountWhenClientReturnsZero() {
            LocalDate to = LocalDate.now();
            LocalDate from = PeriodType.DAILY.getStartDate(to);

            when(reportClient.sumRevenues(USER_ID, from, to)).thenReturn(BigDecimal.ZERO);

            ReportDto result = reportSummaryService.sumRevenue(USER_ID, PeriodType.DAILY);

            assertThat(result.amount()).isEqualByComparingTo("0");
        }
    }
}
