package com.finovara.reportservice.sharedaccount.report.finances.calculate.sum.service;

import com.finovara.contracts.model.PeriodType;
import com.finovara.reportservice.feignclient.FinanceBackendSharedReportClient;
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
class SharedReportSummaryServiceTest {

    private static final Long OWNER_ID = 1L;
    private static final Long MEMBER_ID = 2L;

    @Mock
    private FinanceBackendSharedReportClient reportClient;

    private Clock clock;
    private SharedReportSummaryService sharedReportSummaryService;

    @BeforeEach
    void setUp() {
        clock = Clock.fixed(Instant.parse("2026-07-11T00:00:00Z"), ZoneOffset.UTC);
        sharedReportSummaryService = new SharedReportSummaryService(reportClient, clock);
    }

    @Nested
    class SumExpense {

        @ParameterizedTest
        @EnumSource(PeriodType.class)
        void shouldDelegateToClientAndReturnDto(PeriodType periodType) {
            LocalDate to = LocalDate.now(clock);
            LocalDate from = periodType.getStartDate(to);

            when(reportClient.sumExpenses(OWNER_ID, MEMBER_ID, from, to)).thenReturn(BigDecimal.valueOf(100));

            ReportDto result = sharedReportSummaryService.sumExpense(OWNER_ID, MEMBER_ID, periodType);

            assertThat(result.amount()).isEqualByComparingTo("100");
            assertThat(result.periodType()).isEqualTo(periodType);
            verify(reportClient).sumExpenses(OWNER_ID, MEMBER_ID, from, to);
        }

        @Test
        void shouldReturnZeroAmountWhenClientReturnsZero() {
            LocalDate to = LocalDate.now(clock);
            LocalDate from = PeriodType.DAILY.getStartDate(to);

            when(reportClient.sumExpenses(OWNER_ID, MEMBER_ID, from, to)).thenReturn(BigDecimal.ZERO);

            ReportDto result = sharedReportSummaryService.sumExpense(OWNER_ID, MEMBER_ID, PeriodType.DAILY);

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

            when(reportClient.sumRevenues(OWNER_ID, MEMBER_ID, from, to)).thenReturn(BigDecimal.valueOf(200));

            ReportDto result = sharedReportSummaryService.sumRevenue(OWNER_ID, MEMBER_ID, periodType);

            assertThat(result.amount()).isEqualByComparingTo("200");
            assertThat(result.periodType()).isEqualTo(periodType);
            verify(reportClient).sumRevenues(OWNER_ID, MEMBER_ID, from, to);
        }

        @Test
        void shouldReturnZeroAmountWhenClientReturnsZero() {
            LocalDate to = LocalDate.now(clock);
            LocalDate from = PeriodType.DAILY.getStartDate(to);

            when(reportClient.sumRevenues(OWNER_ID, MEMBER_ID, from, to)).thenReturn(BigDecimal.ZERO);

            ReportDto result = sharedReportSummaryService.sumRevenue(OWNER_ID, MEMBER_ID, PeriodType.DAILY);

            assertThat(result.amount()).isEqualByComparingTo("0");
        }
    }

    @Nested
    class SumAllExpenses {

        @Test
        void shouldReturnTotalExpensesForOwnerAndMember() {
            when(reportClient.sumAllExpenses(OWNER_ID, MEMBER_ID)).thenReturn(BigDecimal.valueOf(1200));

            BigDecimal result = sharedReportSummaryService.sumAllExpenses(OWNER_ID, MEMBER_ID);

            assertThat(result).isEqualByComparingTo("1200");
        }

        @Test
        void shouldCallClientWithOwnerAndMemberIds() {
            when(reportClient.sumAllExpenses(OWNER_ID, MEMBER_ID)).thenReturn(BigDecimal.ZERO);

            sharedReportSummaryService.sumAllExpenses(OWNER_ID, MEMBER_ID);

            verify(reportClient).sumAllExpenses(OWNER_ID, MEMBER_ID);
        }

        @Test
        void shouldReturnZeroWhenNoExpensesExist() {
            when(reportClient.sumAllExpenses(OWNER_ID, MEMBER_ID)).thenReturn(BigDecimal.ZERO);

            BigDecimal result = sharedReportSummaryService.sumAllExpenses(OWNER_ID, MEMBER_ID);

            assertThat(result).isEqualByComparingTo("0");
        }
    }

    @Nested
    class SumAllRevenues {

        @Test
        void shouldReturnTotalRevenuesForOwnerAndMember() {
            when(reportClient.sumAllRevenues(OWNER_ID, MEMBER_ID)).thenReturn(BigDecimal.valueOf(5000));

            BigDecimal result = sharedReportSummaryService.sumAllRevenues(OWNER_ID, MEMBER_ID);

            assertThat(result).isEqualByComparingTo("5000");
        }

        @Test
        void shouldCallClientWithOwnerAndMemberIds() {
            when(reportClient.sumAllRevenues(OWNER_ID, MEMBER_ID)).thenReturn(BigDecimal.ZERO);

            sharedReportSummaryService.sumAllRevenues(OWNER_ID, MEMBER_ID);

            verify(reportClient).sumAllRevenues(OWNER_ID, MEMBER_ID);
        }

        @Test
        void shouldReturnZeroWhenNoRevenuesExist() {
            when(reportClient.sumAllRevenues(OWNER_ID, MEMBER_ID)).thenReturn(BigDecimal.ZERO);

            BigDecimal result = sharedReportSummaryService.sumAllRevenues(OWNER_ID, MEMBER_ID);

            assertThat(result).isEqualByComparingTo("0");
        }
    }
}