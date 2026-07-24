package com.finovara.reportservice.report.finances.calculate.sum;

import com.finovara.contracts.model.PeriodType;
import com.finovara.reportservice.feignclient.FinanceBackendReportClient;
import com.finovara.reportservice.util.dto.ReportDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportSummaryServiceTest {

    private static final Long USER_ID = 1L;

    @Mock
    private FinanceBackendReportClient reportClient;

    private Clock clock;
    private ReportSummaryService reportSummaryService;

    @BeforeEach
    void setUp() {
        clock = Clock.fixed(Instant.parse("2026-07-11T00:00:00Z"), ZoneOffset.UTC);
        reportSummaryService = new ReportSummaryService(reportClient, clock);
    }

    @Nested
    class SumExpense {

        @ParameterizedTest
        @EnumSource(PeriodType.class)
        void shouldDelegateToClientAndReturnDto(PeriodType periodType) {
            LocalDate to = LocalDate.now(clock);
            LocalDate from = periodType.getStartDate(to);

            when(reportClient.sumExpenses(USER_ID, from, to)).thenReturn(BigDecimal.valueOf(100));

            ReportDto result = reportSummaryService.sumExpense(USER_ID, periodType);

            assertThat(result.amount()).isEqualByComparingTo("100");
            assertThat(result.periodType()).isEqualTo(periodType);
            verify(reportClient).sumExpenses(USER_ID, from, to);
        }

        @Test
        void shouldReturnZeroAmountWhenClientReturnsZero() {
            LocalDate to = LocalDate.now(clock);
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
            LocalDate to = LocalDate.now(clock);
            LocalDate from = periodType.getStartDate(to);

            when(reportClient.sumRevenues(USER_ID, from, to)).thenReturn(BigDecimal.valueOf(200));

            ReportDto result = reportSummaryService.sumRevenue(USER_ID, periodType);

            assertThat(result.amount()).isEqualByComparingTo("200");
            assertThat(result.periodType()).isEqualTo(periodType);
            verify(reportClient).sumRevenues(USER_ID, from, to);
        }

        @Test
        void shouldReturnZeroAmountWhenClientReturnsZero() {
            LocalDate to = LocalDate.now(clock);
            LocalDate from = PeriodType.DAILY.getStartDate(to);

            when(reportClient.sumRevenues(USER_ID, from, to)).thenReturn(BigDecimal.ZERO);

            ReportDto result = reportSummaryService.sumRevenue(USER_ID, PeriodType.DAILY);

            assertThat(result.amount()).isEqualByComparingTo("0");
        }
    }

    @Nested
    class SumAllExpenses {

        @Test
        void shouldReturnTotalExpensesForUser() {
            when(reportClient.sumAllExpenses(USER_ID)).thenReturn(BigDecimal.valueOf(1200));

            BigDecimal result = reportSummaryService.sumAllExpenses(USER_ID);

            assertThat(result).isEqualByComparingTo("1200");
        }

        @Test
        void shouldCallClientWithCorrectUserId() {
            when(reportClient.sumAllExpenses(USER_ID)).thenReturn(BigDecimal.ZERO);

            reportSummaryService.sumAllExpenses(USER_ID);

            verify(reportClient).sumAllExpenses(USER_ID);
        }

        @Test
        void shouldReturnZeroWhenUserHasNoExpenses() {
            when(reportClient.sumAllExpenses(USER_ID)).thenReturn(BigDecimal.ZERO);

            BigDecimal result = reportSummaryService.sumAllExpenses(USER_ID);

            assertThat(result).isEqualByComparingTo("0");
        }

        @Test
        void shouldReturnNegativeValueWhenClientReturnsNegativeAmount() {
            when(reportClient.sumAllExpenses(USER_ID)).thenReturn(BigDecimal.valueOf(-50));

            BigDecimal result = reportSummaryService.sumAllExpenses(USER_ID);

            assertThat(result).isEqualByComparingTo("-50");
        }
    }

    @Nested
    class SumAllRevenues {

        @Test
        void shouldReturnTotalRevenuesForUser() {
            when(reportClient.sumAllRevenues(USER_ID)).thenReturn(BigDecimal.valueOf(5000));

            BigDecimal result = reportSummaryService.sumAllRevenues(USER_ID);

            assertThat(result).isEqualByComparingTo("5000");
        }

        @Test
        void shouldCallClientWithCorrectUserId() {
            when(reportClient.sumAllRevenues(USER_ID)).thenReturn(BigDecimal.ZERO);

            reportSummaryService.sumAllRevenues(USER_ID);

            verify(reportClient).sumAllRevenues(USER_ID);
        }

        @Test
        void shouldReturnZeroWhenUserHasNoRevenues() {
            when(reportClient.sumAllRevenues(USER_ID)).thenReturn(BigDecimal.ZERO);

            BigDecimal result = reportSummaryService.sumAllRevenues(USER_ID);

            assertThat(result).isEqualByComparingTo("0");
        }
    }
}