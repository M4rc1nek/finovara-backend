package com.finovara.reportservice.report.finances.response.service;

import com.finovara.contracts.exception.badrequest.InvalidInputException;
import com.finovara.contracts.model.PeriodType;
import com.finovara.contracts.model.transaction.ExpenseCategory;
import com.finovara.contracts.model.transaction.RevenueCategory;
import com.finovara.contracts.transaction.report.dto.HighestExpenseDto;
import com.finovara.contracts.transaction.report.dto.HighestRevenueDto;
import com.finovara.reportservice.report.finances.calculate.categorypercentage.expense.dto.ExpenseCategoryPercentageDto;
import com.finovara.reportservice.report.finances.calculate.categorypercentage.expense.service.ExpenseCategoryPercentageService;
import com.finovara.reportservice.report.finances.calculate.categorypercentage.revenue.dto.RevenueCategoryPercentageDto;
import com.finovara.reportservice.report.finances.calculate.categorypercentage.revenue.service.RevenueCategoryPercentageService;
import com.finovara.reportservice.report.finances.calculate.chart.averagecashflow.service.AverageCashFlowChartService;
import com.finovara.reportservice.report.finances.calculate.chart.cashflow.service.TotalCashFlowChartService;
import com.finovara.reportservice.report.finances.calculate.highesttransactions.highestexpense.service.HighestExpenseService;
import com.finovara.reportservice.report.finances.calculate.highesttransactions.highestrevenue.service.HighestRevenueService;
import com.finovara.reportservice.report.finances.calculate.sum.ReportSummaryService;
import com.finovara.reportservice.util.dto.ReportDto;
import com.finovara.reportservice.util.dto.financial.FinancialReportDto;
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
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FinancialReportServiceTest {

    private static final Long USER_ID = 1L;

    @Mock
    private ReportSummaryService summaryService;

    @Mock
    private HighestExpenseService highestExpenseService;

    @Mock
    private HighestRevenueService highestRevenueService;

    @Mock
    private ExpenseCategoryPercentageService expenseCategoryPercentageService;

    @Mock
    private RevenueCategoryPercentageService revenueCategoryPercentageService;

    @Mock
    private TotalCashFlowChartService totalCashFlowChartService;

    @Mock
    private AverageCashFlowChartService averageCashFlowChartService;

    private Clock clock;
    private FinancialReportService financialReportService;

    @BeforeEach
    void setUp() {
        clock = Clock.fixed(Instant.parse("2026-07-11T00:00:00Z"), ZoneOffset.UTC);
        financialReportService = new FinancialReportService(
                summaryService,
                highestExpenseService,
                highestRevenueService,
                expenseCategoryPercentageService,
                revenueCategoryPercentageService,
                totalCashFlowChartService,
                averageCashFlowChartService,
                clock
        );
    }

    private void stubTotals() {
        when(summaryService.sumAllExpenses(USER_ID)).thenReturn(BigDecimal.valueOf(500));
        when(summaryService.sumAllRevenues(USER_ID)).thenReturn(BigDecimal.valueOf(800));
    }

    private void stubPeriodSummary() {
        when(summaryService.sumExpense(USER_ID, PeriodType.DAILY)).thenReturn(new ReportDto(PeriodType.DAILY, BigDecimal.ONE));
        when(summaryService.sumExpense(USER_ID, PeriodType.WEEKLY)).thenReturn(new ReportDto(PeriodType.WEEKLY, BigDecimal.TEN));
        when(summaryService.sumExpense(USER_ID, PeriodType.MONTHLY)).thenReturn(new ReportDto(PeriodType.MONTHLY, BigDecimal.valueOf(100)));
        when(summaryService.sumRevenue(USER_ID, PeriodType.DAILY)).thenReturn(new ReportDto(PeriodType.DAILY, BigDecimal.valueOf(2)));
        when(summaryService.sumRevenue(USER_ID, PeriodType.WEEKLY)).thenReturn(new ReportDto(PeriodType.WEEKLY, BigDecimal.valueOf(20)));
        when(summaryService.sumRevenue(USER_ID, PeriodType.MONTHLY)).thenReturn(new ReportDto(PeriodType.MONTHLY, BigDecimal.valueOf(200)));
    }

    private void stubHighestTransactions(PeriodType periodType, List<HighestExpenseDto> expenses, List<HighestRevenueDto> revenues) {
        when(highestExpenseService.getHighestExpense(USER_ID, periodType)).thenReturn(expenses);
        when(highestRevenueService.getHighestRevenue(USER_ID, periodType)).thenReturn(revenues);
    }

    private void stubCategoryPercentages(PeriodType periodType) {
        when(expenseCategoryPercentageService.getExpensePercentageByCategoryReport(eq(USER_ID), any(ExpenseCategory.class), eq(periodType)))
                .thenAnswer(invocation -> new ExpenseCategoryPercentageDto(BigDecimal.TEN, invocation.getArgument(1)));
        when(revenueCategoryPercentageService.getRevenuePercentageByCategoryReport(eq(USER_ID), any(RevenueCategory.class), eq(periodType)))
                .thenAnswer(invocation -> new RevenueCategoryPercentageDto(BigDecimal.TEN, invocation.getArgument(1)));
    }

    private void stubCashFlowCharts(LocalDate today) {
        when(totalCashFlowChartService.getCashFlowChart(USER_ID, today)).thenReturn(Collections.emptyList());
        when(averageCashFlowChartService.getAverageCashFlowChart(USER_ID, today)).thenReturn(Collections.emptyList());
    }

    private void stubHappyPath(PeriodType periodType) {
        stubTotals();
        stubPeriodSummary();
        stubHighestTransactions(periodType, List.of(mock(HighestExpenseDto.class)), List.of(mock(HighestRevenueDto.class)));
        stubCategoryPercentages(periodType);
        stubCashFlowCharts(LocalDate.now(clock));
    }

    @Nested
    class Validation {

        @Test
        void shouldThrowExceptionWhenPeriodTypeIsNull() {
            assertThrows(InvalidInputException.class, () -> financialReportService.getFinancialReport(USER_ID, null));

            verifyNoInteractions(summaryService, highestExpenseService, highestRevenueService,
                    expenseCategoryPercentageService, revenueCategoryPercentageService,
                    totalCashFlowChartService, averageCashFlowChartService);
        }
    }

    @Nested
    class GetFinancialReport {

        @ParameterizedTest
        @EnumSource(PeriodType.class)
        void shouldReturnReportWithRequestedPeriodTypeForEveryPeriodType(PeriodType periodType) {
            stubHappyPath(periodType);

            FinancialReportDto result = financialReportService.getFinancialReport(USER_ID, periodType);

            assertThat(result).isNotNull();
            assertThat(result.periodType()).isEqualTo(periodType);
        }

        @Test
        void shouldReturnHighestExpensesAndRevenuesFromRespectiveServices() {
            List<HighestExpenseDto> expenses = List.of(mock(HighestExpenseDto.class), mock(HighestExpenseDto.class));
            List<HighestRevenueDto> revenues = List.of(mock(HighestRevenueDto.class));
            stubTotals();
            stubPeriodSummary();
            stubHighestTransactions(PeriodType.MONTHLY, expenses, revenues);
            stubCategoryPercentages(PeriodType.MONTHLY);
            stubCashFlowCharts(LocalDate.now(clock));

            FinancialReportDto result = financialReportService.getFinancialReport(USER_ID, PeriodType.MONTHLY);

            assertThat(result.highestExpenses()).isSameAs(expenses);
            assertThat(result.highestRevenues()).isSameAs(revenues);
        }

        @Test
        void shouldFetchHighestTransactionsUsingRequestedPeriodType() {
            stubHappyPath(PeriodType.WEEKLY);

            financialReportService.getFinancialReport(USER_ID, PeriodType.WEEKLY);

            verify(highestExpenseService).getHighestExpense(USER_ID, PeriodType.WEEKLY);
            verify(highestRevenueService).getHighestRevenue(USER_ID, PeriodType.WEEKLY);
        }

        @Test
        void shouldFetchCashFlowChartsUsingTodayFromClock() {
            stubHappyPath(PeriodType.DAILY);
            LocalDate today = LocalDate.now(clock);

            financialReportService.getFinancialReport(USER_ID, PeriodType.DAILY);

            verify(totalCashFlowChartService).getCashFlowChart(USER_ID, today);
            verify(averageCashFlowChartService).getAverageCashFlowChart(USER_ID, today);
        }

        @Test
        void shouldFetchAllThreePeriodTypesForExpenseAndRevenueSummary() {
            stubHappyPath(PeriodType.MONTHLY);

            financialReportService.getFinancialReport(USER_ID, PeriodType.MONTHLY);

            verify(summaryService).sumExpense(USER_ID, PeriodType.DAILY);
            verify(summaryService).sumExpense(USER_ID, PeriodType.WEEKLY);
            verify(summaryService).sumExpense(USER_ID, PeriodType.MONTHLY);
            verify(summaryService).sumRevenue(USER_ID, PeriodType.DAILY);
            verify(summaryService).sumRevenue(USER_ID, PeriodType.WEEKLY);
            verify(summaryService).sumRevenue(USER_ID, PeriodType.MONTHLY);
        }

        @Test
        void shouldFetchTotalExpensesAndRevenuesOnce() {
            stubHappyPath(PeriodType.MONTHLY);

            financialReportService.getFinancialReport(USER_ID, PeriodType.MONTHLY);

            verify(summaryService, times(1)).sumAllExpenses(USER_ID);
            verify(summaryService, times(1)).sumAllRevenues(USER_ID);
        }

        @Test
        void shouldRequestExpensePercentageForEveryExpenseCategory() {
            stubHappyPath(PeriodType.MONTHLY);

            financialReportService.getFinancialReport(USER_ID, PeriodType.MONTHLY);

            for (ExpenseCategory category : ExpenseCategory.values()) {
                verify(expenseCategoryPercentageService)
                        .getExpensePercentageByCategoryReport(USER_ID, category, PeriodType.MONTHLY);
            }
        }

        @Test
        void shouldRequestRevenuePercentageForEveryRevenueCategory() {
            stubHappyPath(PeriodType.MONTHLY);

            financialReportService.getFinancialReport(USER_ID, PeriodType.MONTHLY);

            for (RevenueCategory category : RevenueCategory.values()) {
                verify(revenueCategoryPercentageService)
                        .getRevenuePercentageByCategoryReport(USER_ID, category, PeriodType.MONTHLY);
            }
        }

        @Test
        void shouldReturnEmptyExpensePercentagesWhenNoExpenseCategoriesExist() {
            stubHappyPath(PeriodType.DAILY);

            FinancialReportDto result = financialReportService.getFinancialReport(USER_ID, PeriodType.DAILY);

            assertThat(result.expenseCategoryPercentages()).hasSize(ExpenseCategory.values().length);
        }

        @Test
        void shouldReturnRevenuePercentagesMatchingRevenueCategoryCount() {
            stubHappyPath(PeriodType.DAILY);

            FinancialReportDto result = financialReportService.getFinancialReport(USER_ID, PeriodType.DAILY);

            assertThat(result.revenueCategoryPercentages()).hasSize(RevenueCategory.values().length);
        }

        @Test
        void shouldReturnEmptyCashFlowWhenChartServicesReturnEmptyLists() {
            stubHappyPath(PeriodType.DAILY);

            FinancialReportDto result = financialReportService.getFinancialReport(USER_ID, PeriodType.DAILY);

            assertThat(result.cashFlowChart()).isEmpty();
            assertThat(result.averageCashFlowChart()).isEmpty();
        }
    }
}