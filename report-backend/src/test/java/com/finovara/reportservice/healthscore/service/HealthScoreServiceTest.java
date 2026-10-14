package com.finovara.reportservice.healthscore.service;

import static java.util.Collections.emptyList;
import static java.util.Collections.nCopies;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.finovara.contracts.mainaccount.report.financial.dto.FinancialReportDto;
import com.finovara.contracts.mainaccount.report.financial.dto.FinancialTotalsDto;
import com.finovara.contracts.mainaccount.report.financial.dto.HighestExpenseDto;
import com.finovara.contracts.util.PeriodType;
import com.finovara.reportservice.feignclient.FinanceBackendReportClient;
import com.finovara.reportservice.healthscore.dto.HealthScoreDto;
import com.finovara.reportservice.healthscore.model.HealthScoreStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

@ExtendWith(MockitoExtension.class)
class HealthScoreServiceTest {

    private static final Long USER_ID = 1L;
    private static final PeriodType REPORT_PERIOD = PeriodType.MONTHLY;

    @Mock
    private FinanceBackendReportClient financeBackendReportClient;

    @Mock
    private FinancialReportDto report;

    @Mock
    private FinancialTotalsDto totals;

    @Mock
    private HighestExpenseDto highestExpense;

    @Mock
    private HighestExpenseDto smallExpense;

    @Mock
    private HighestExpenseDto expenseWithoutAmount;

    @InjectMocks
    private HealthScoreService service;

    @Nested
    class GetHealthScore {

        @BeforeEach
        void setUp() {
            when(financeBackendReportClient.getFinancialReport(USER_ID, REPORT_PERIOD)).thenReturn(report);
            when(report.totals()).thenReturn(totals);
        }

        @ParameterizedTest
        @CsvSource({
                "1000,500,5000,100,95,EXCELLENT",
                "2000,1000,5000,600,85,EXCELLENT",
                "2000,1000,5000,640,84,GOOD",
                "2000,1000,2580,1000,70,GOOD",
                "2000,1000,2520,1000,69,NEEDS_ATTENTION",
                "2000,1000,0,600,50,NEEDS_ATTENTION",
                "2000,1000,0,640,49,POOR",
                "1000,2000,0,2000,0,POOR",
                "1000,500,5000,1000,75,GOOD",
                "0,500,5000,100,55,NEEDS_ATTENTION"
        })
        void shouldReturnExpectedScoreAndStatusWhenFinancialDataIsGiven(
                BigDecimal revenues,
                BigDecimal expenses,
                BigDecimal walletBalance,
                BigDecimal topExpense,
                BigDecimal expectedScore,
                HealthScoreStatus expectedStatus) {
            when(totals.revenues()).thenReturn(revenues);
            when(totals.expenses()).thenReturn(expenses);
            when(financeBackendReportClient.walletBalance(USER_ID)).thenReturn(walletBalance);
            when(highestExpense.amount()).thenReturn(topExpense);
            when(report.highestExpenses()).thenReturn(List.of(highestExpense));

            HealthScoreDto result = service.getHealthScore(USER_ID);

            assertEquals(new HealthScoreDto(expectedScore, expectedStatus), result);
        }

        @Test
        void shouldReturnExcellentWhenExpensesAreZeroAndRevenuesArePositive() {
            when(totals.revenues()).thenReturn(new BigDecimal("1000"));
            when(totals.expenses()).thenReturn(BigDecimal.ZERO);
            when(financeBackendReportClient.walletBalance(USER_ID)).thenReturn(new BigDecimal("100"));

            HealthScoreDto result = service.getHealthScore(USER_ID);

            assertEquals(new HealthScoreDto(BigDecimal.valueOf(100), HealthScoreStatus.EXCELLENT), result);
        }

        @Test
        void shouldTreatNullRevenuesAndExpensesAsZeroWhenTotalsHaveNoValues() {
            HealthScoreDto result = service.getHealthScore(USER_ID);

            assertEquals(new HealthScoreDto(BigDecimal.valueOf(60), HealthScoreStatus.NEEDS_ATTENTION), result);
        }

        @Test
        void shouldTreatNullWalletBalanceAsZeroWhenWalletBalanceIsNull() {
            when(totals.revenues()).thenReturn(new BigDecimal("2000"));
            when(totals.expenses()).thenReturn(new BigDecimal("1000"));

            HealthScoreDto result = service.getHealthScore(USER_ID);

            assertEquals(new HealthScoreDto(BigDecimal.valueOf(65), HealthScoreStatus.NEEDS_ATTENTION), result);
        }

        @Test
        void shouldReturnZeroEmergencyFundScoreWhenWalletBalanceIsNegative() {
            when(totals.revenues()).thenReturn(new BigDecimal("2000"));
            when(totals.expenses()).thenReturn(new BigDecimal("1000"));
            when(financeBackendReportClient.walletBalance(USER_ID)).thenReturn(new BigDecimal("-500"));

            HealthScoreDto result = service.getHealthScore(USER_ID);

            assertEquals(new HealthScoreDto(BigDecimal.valueOf(65), HealthScoreStatus.NEEDS_ATTENTION), result);
        }

        @Test
        void shouldReturnFullDiversityScoreWhenHighestExpensesAreEmpty() {
            when(totals.revenues()).thenReturn(new BigDecimal("2000"));
            when(totals.expenses()).thenReturn(new BigDecimal("1000"));
            when(financeBackendReportClient.walletBalance(USER_ID)).thenReturn(new BigDecimal("3000"));
            when(report.highestExpenses()).thenReturn(emptyList());

            HealthScoreDto result = service.getHealthScore(USER_ID);

            assertEquals(new HealthScoreDto(BigDecimal.valueOf(100), HealthScoreStatus.EXCELLENT), result);
        }

        @Test
        void shouldReturnFullDiversityScoreWhenHighestExpensesAreNull() {
            when(totals.revenues()).thenReturn(new BigDecimal("2000"));
            when(totals.expenses()).thenReturn(new BigDecimal("1000"));
            when(financeBackendReportClient.walletBalance(USER_ID)).thenReturn(new BigDecimal("3000"));
            when(report.highestExpenses()).thenReturn(null);

            HealthScoreDto result = service.getHealthScore(USER_ID);

            assertEquals(new HealthScoreDto(BigDecimal.valueOf(100), HealthScoreStatus.EXCELLENT), result);
        }

        @Test
        void shouldIgnoreExpensesWithNullAmountWhenCalculatingDiversityScore() {
            when(totals.revenues()).thenReturn(new BigDecimal("2000"));
            when(totals.expenses()).thenReturn(new BigDecimal("1000"));
            when(financeBackendReportClient.walletBalance(USER_ID)).thenReturn(new BigDecimal("3000"));
            when(highestExpense.amount()).thenReturn(new BigDecimal("100"));
            when(report.highestExpenses()).thenReturn(List.of(expenseWithoutAmount, highestExpense));

            HealthScoreDto result = service.getHealthScore(USER_ID);

            assertEquals(new HealthScoreDto(BigDecimal.valueOf(98), HealthScoreStatus.EXCELLENT), result);
        }

        @Test
        void shouldSumOnlyFiveHighestExpensesWhenMoreExpensesAreReturned() {
            when(totals.revenues()).thenReturn(new BigDecimal("6000"));
            when(totals.expenses()).thenReturn(new BigDecimal("3000"));
            when(financeBackendReportClient.walletBalance(USER_ID)).thenReturn(new BigDecimal("9000"));
            when(highestExpense.amount()).thenReturn(new BigDecimal("100"));
            when(report.highestExpenses()).thenReturn(nCopies(10, highestExpense));

            HealthScoreDto result = service.getHealthScore(USER_ID);

            assertEquals(new HealthScoreDto(BigDecimal.valueOf(96), HealthScoreStatus.EXCELLENT), result);
        }

        @Test
        void shouldSortExpensesDescendingBeforeLimitingWhenExpensesAreUnordered() {
            when(totals.revenues()).thenReturn(new BigDecimal("10000"));
            when(totals.expenses()).thenReturn(new BigDecimal("5000"));
            when(financeBackendReportClient.walletBalance(USER_ID)).thenReturn(new BigDecimal("15000"));
            when(smallExpense.amount()).thenReturn(BigDecimal.ONE);
            when(highestExpense.amount()).thenReturn(new BigDecimal("1000"));
            when(report.highestExpenses()).thenReturn(List.of(
                    smallExpense, smallExpense, smallExpense, smallExpense, smallExpense,
                    highestExpense, highestExpense, highestExpense, highestExpense, highestExpense));

            HealthScoreDto result = service.getHealthScore(USER_ID);

            assertEquals(new HealthScoreDto(BigDecimal.valueOf(75), HealthScoreStatus.GOOD), result);
        }

        @Test
        void shouldFetchReportAndWalletBalanceExactlyOnceWhenScoreIsCalculated() {
            service.getHealthScore(USER_ID);

            verify(financeBackendReportClient, times(1)).getFinancialReport(USER_ID, REPORT_PERIOD);
            verify(financeBackendReportClient, times(1)).walletBalance(USER_ID);
            verifyNoMoreInteractions(financeBackendReportClient);
        }
    }

    @Nested
    class GetHealthScoreWithMissingReportData {

        @Test
        void shouldReturnNeedsAttentionWhenReportIsNull() {
            when(financeBackendReportClient.getFinancialReport(USER_ID, REPORT_PERIOD)).thenReturn(null);

            HealthScoreDto result = service.getHealthScore(USER_ID);

            assertEquals(new HealthScoreDto(BigDecimal.valueOf(60), HealthScoreStatus.NEEDS_ATTENTION), result);
        }

        @Test
        void shouldReturnNeedsAttentionWhenTotalsAreNull() {
            when(financeBackendReportClient.getFinancialReport(USER_ID, REPORT_PERIOD)).thenReturn(report);

            HealthScoreDto result = service.getHealthScore(USER_ID);

            assertEquals(new HealthScoreDto(BigDecimal.valueOf(60), HealthScoreStatus.NEEDS_ATTENTION), result);
        }
    }

    @Nested
    class GetHealthScoreWithClientFailures {

        @Test
        void shouldThrowExceptionWhenReportClientFails() {
            when(financeBackendReportClient.getFinancialReport(USER_ID, REPORT_PERIOD)).thenThrow(new IllegalStateException("client error"));

            assertThrows(IllegalStateException.class, () -> service.getHealthScore(USER_ID));

            verify(financeBackendReportClient, never()).walletBalance(USER_ID);
        }

        @Test
        void shouldThrowExceptionWhenWalletBalanceClientFails() {
            when(financeBackendReportClient.getFinancialReport(USER_ID, REPORT_PERIOD)).thenReturn(report);
            when(financeBackendReportClient.walletBalance(USER_ID)).thenThrow(new IllegalStateException("client error"));

            assertThrows(IllegalStateException.class, () -> service.getHealthScore(USER_ID));
        }
    }
}