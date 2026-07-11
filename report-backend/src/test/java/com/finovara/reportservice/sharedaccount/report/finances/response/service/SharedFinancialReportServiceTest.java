package com.finovara.reportservice.sharedaccount.report.finances.response.service;

import com.finovara.contracts.exception.badrequest.InvalidInputException;
import com.finovara.contracts.model.PeriodType;
import com.finovara.contracts.model.transaction.ExpenseCategory;
import com.finovara.contracts.model.transaction.RevenueCategory;
import com.finovara.contracts.transaction.report.dto.HighestExpenseDto;
import com.finovara.contracts.transaction.report.dto.HighestRevenueDto;
import com.finovara.reportservice.sharedaccount.report.finances.calculate.categorypercentage.expense.dto.SharedExpenseCategoryPercentageDto;
import com.finovara.reportservice.sharedaccount.report.finances.calculate.categorypercentage.expense.service.SharedExpenseCategoryPercentageService;
import com.finovara.reportservice.sharedaccount.report.finances.calculate.categorypercentage.revenue.dto.SharedRevenueCategoryPercentageDto;
import com.finovara.reportservice.sharedaccount.report.finances.calculate.categorypercentage.revenue.service.SharedRevenueCategoryPercentageService;
import com.finovara.reportservice.sharedaccount.report.finances.calculate.chart.averagecashflow.service.SharedAverageCashFlowChartService;
import com.finovara.reportservice.sharedaccount.report.finances.calculate.chart.cashflow.service.SharedTotalCashFlowChartService;
import com.finovara.reportservice.sharedaccount.report.finances.calculate.highesttransactions.highestexpense.service.SharedHighestExpenseService;
import com.finovara.reportservice.sharedaccount.report.finances.calculate.highesttransactions.highestrevenue.service.SharedHighestRevenueService;
import com.finovara.reportservice.sharedaccount.report.finances.calculate.sum.service.SharedReportSummaryService;
import com.finovara.reportservice.util.dto.ReportDto;
import com.finovara.reportservice.util.dto.financial.FinancialReportDto;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
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
class SharedFinancialReportServiceTest {

    private static final Long OWNER_ID = 1L;
    private static final Long MEMBER_ID = 2L;

    @Mock
    private SharedReportSummaryService summaryService;

    @Mock
    private SharedHighestExpenseService highestExpenseService;

    @Mock
    private SharedHighestRevenueService highestRevenueService;

    @Mock
    private SharedExpenseCategoryPercentageService expenseCategoryPercentageService;

    @Mock
    private SharedRevenueCategoryPercentageService revenueCategoryPercentageService;

    @Mock
    private SharedTotalCashFlowChartService totalCashFlowChartService;

    @Mock
    private SharedAverageCashFlowChartService averageCashFlowChartService;

    @InjectMocks
    private SharedFinancialReportService sharedFinancialReportService;

    private void stubTotals() {
        when(summaryService.sumAllExpenses(OWNER_ID, MEMBER_ID)).thenReturn(BigDecimal.valueOf(500));
        when(summaryService.sumAllRevenues(OWNER_ID, MEMBER_ID)).thenReturn(BigDecimal.valueOf(800));
    }

    private void stubPeriodSummary() {
        when(summaryService.sumExpense(OWNER_ID, MEMBER_ID, PeriodType.DAILY)).thenReturn(new ReportDto(PeriodType.DAILY, BigDecimal.ONE));
        when(summaryService.sumExpense(OWNER_ID, MEMBER_ID, PeriodType.WEEKLY)).thenReturn(new ReportDto(PeriodType.WEEKLY, BigDecimal.TEN));
        when(summaryService.sumExpense(OWNER_ID, MEMBER_ID, PeriodType.MONTHLY)).thenReturn(new ReportDto(PeriodType.MONTHLY, BigDecimal.valueOf(100)));
        when(summaryService.sumRevenue(OWNER_ID, MEMBER_ID, PeriodType.DAILY)).thenReturn(new ReportDto(PeriodType.DAILY, BigDecimal.valueOf(2)));
        when(summaryService.sumRevenue(OWNER_ID, MEMBER_ID, PeriodType.WEEKLY)).thenReturn(new ReportDto(PeriodType.WEEKLY, BigDecimal.valueOf(20)));
        when(summaryService.sumRevenue(OWNER_ID, MEMBER_ID, PeriodType.MONTHLY)).thenReturn(new ReportDto(PeriodType.MONTHLY, BigDecimal.valueOf(200)));
    }

    private void stubHighestTransactions(PeriodType periodType, List<HighestExpenseDto> expenses, List<HighestRevenueDto> revenues) {
        when(highestExpenseService.getHighestExpense(OWNER_ID, MEMBER_ID, periodType)).thenReturn(expenses);
        when(highestRevenueService.getHighestRevenue(OWNER_ID, MEMBER_ID, periodType)).thenReturn(revenues);
    }

    private void stubCategoryPercentages(PeriodType periodType) {
        when(expenseCategoryPercentageService.getExpensePercentageByCategoryReport(eq(OWNER_ID), eq(MEMBER_ID), any(ExpenseCategory.class), eq(periodType)))
                .thenAnswer(invocation -> new SharedExpenseCategoryPercentageDto(BigDecimal.TEN, invocation.getArgument(2)));
        when(revenueCategoryPercentageService.getRevenuePercentageByCategoryReport(eq(OWNER_ID), eq(MEMBER_ID), any(RevenueCategory.class), eq(periodType)))
                .thenAnswer(invocation -> new SharedRevenueCategoryPercentageDto(BigDecimal.TEN, invocation.getArgument(2)));
    }

    private void stubCashFlowCharts() {
        when(totalCashFlowChartService.getCashFlowChart(OWNER_ID, MEMBER_ID)).thenReturn(Collections.emptyList());
        when(averageCashFlowChartService.getAverageCashFlowChart(OWNER_ID, MEMBER_ID)).thenReturn(Collections.emptyList());
    }

    private void stubHappyPath(PeriodType periodType) {
        stubTotals();
        stubPeriodSummary();
        stubHighestTransactions(periodType, List.of(mock(HighestExpenseDto.class)), List.of(mock(HighestRevenueDto.class)));
        stubCategoryPercentages(periodType);
        stubCashFlowCharts();
    }

    @Nested
    class Validation {

        @Test
        void shouldThrowExceptionWhenPeriodTypeIsNull() {
            assertThrows(InvalidInputException.class,
                    () -> sharedFinancialReportService.getFinancialReport(OWNER_ID, MEMBER_ID, null));

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

            FinancialReportDto result = sharedFinancialReportService.getFinancialReport(OWNER_ID, MEMBER_ID, periodType);

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
            stubCashFlowCharts();

            FinancialReportDto result = sharedFinancialReportService.getFinancialReport(OWNER_ID, MEMBER_ID, PeriodType.MONTHLY);

            assertThat(result.highestExpenses()).isSameAs(expenses);
            assertThat(result.highestRevenues()).isSameAs(revenues);
        }

        @Test
        void shouldFetchHighestTransactionsUsingRequestedPeriodType() {
            stubHappyPath(PeriodType.WEEKLY);

            sharedFinancialReportService.getFinancialReport(OWNER_ID, MEMBER_ID, PeriodType.WEEKLY);

            verify(highestExpenseService).getHighestExpense(OWNER_ID, MEMBER_ID, PeriodType.WEEKLY);
            verify(highestRevenueService).getHighestRevenue(OWNER_ID, MEMBER_ID, PeriodType.WEEKLY);
        }

        @Test
        void shouldFetchCashFlowChartsForOwnerAndMember() {
            stubHappyPath(PeriodType.DAILY);

            sharedFinancialReportService.getFinancialReport(OWNER_ID, MEMBER_ID, PeriodType.DAILY);

            verify(totalCashFlowChartService).getCashFlowChart(OWNER_ID, MEMBER_ID);
            verify(averageCashFlowChartService).getAverageCashFlowChart(OWNER_ID, MEMBER_ID);
        }

        @Test
        void shouldFetchAllThreePeriodTypesForExpenseAndRevenueSummary() {
            stubHappyPath(PeriodType.MONTHLY);

            sharedFinancialReportService.getFinancialReport(OWNER_ID, MEMBER_ID, PeriodType.MONTHLY);

            verify(summaryService).sumExpense(OWNER_ID, MEMBER_ID, PeriodType.DAILY);
            verify(summaryService).sumExpense(OWNER_ID, MEMBER_ID, PeriodType.WEEKLY);
            verify(summaryService).sumExpense(OWNER_ID, MEMBER_ID, PeriodType.MONTHLY);
            verify(summaryService).sumRevenue(OWNER_ID, MEMBER_ID, PeriodType.DAILY);
            verify(summaryService).sumRevenue(OWNER_ID, MEMBER_ID, PeriodType.WEEKLY);
            verify(summaryService).sumRevenue(OWNER_ID, MEMBER_ID, PeriodType.MONTHLY);
        }

        @Test
        void shouldFetchTotalExpensesAndRevenuesOnce() {
            stubHappyPath(PeriodType.MONTHLY);

            sharedFinancialReportService.getFinancialReport(OWNER_ID, MEMBER_ID, PeriodType.MONTHLY);

            verify(summaryService, times(1)).sumAllExpenses(OWNER_ID, MEMBER_ID);
            verify(summaryService, times(1)).sumAllRevenues(OWNER_ID, MEMBER_ID);
        }

        @Test
        void shouldRequestExpensePercentageForEveryExpenseCategory() {
            stubHappyPath(PeriodType.MONTHLY);

            sharedFinancialReportService.getFinancialReport(OWNER_ID, MEMBER_ID, PeriodType.MONTHLY);

            for (ExpenseCategory category : ExpenseCategory.values()) {
                verify(expenseCategoryPercentageService)
                        .getExpensePercentageByCategoryReport(OWNER_ID, MEMBER_ID, category, PeriodType.MONTHLY);
            }
        }

        @Test
        void shouldRequestRevenuePercentageForEveryRevenueCategory() {
            stubHappyPath(PeriodType.MONTHLY);

            sharedFinancialReportService.getFinancialReport(OWNER_ID, MEMBER_ID, PeriodType.MONTHLY);

            for (RevenueCategory category : RevenueCategory.values()) {
                verify(revenueCategoryPercentageService)
                        .getRevenuePercentageByCategoryReport(OWNER_ID, MEMBER_ID, category, PeriodType.MONTHLY);
            }
        }

        @Test
        void shouldReturnExpensePercentagesMatchingExpenseCategoryCount() {
            stubHappyPath(PeriodType.DAILY);

            FinancialReportDto result = sharedFinancialReportService.getFinancialReport(OWNER_ID, MEMBER_ID, PeriodType.DAILY);

            assertThat(result.expenseCategoryPercentages()).hasSize(ExpenseCategory.values().length);
        }

        @Test
        void shouldReturnRevenuePercentagesMatchingRevenueCategoryCount() {
            stubHappyPath(PeriodType.DAILY);

            FinancialReportDto result = sharedFinancialReportService.getFinancialReport(OWNER_ID, MEMBER_ID, PeriodType.DAILY);

            assertThat(result.revenueCategoryPercentages()).hasSize(RevenueCategory.values().length);
        }

        @Test
        void shouldReturnEmptyCashFlowWhenChartServicesReturnEmptyLists() {
            stubHappyPath(PeriodType.DAILY);

            FinancialReportDto result = sharedFinancialReportService.getFinancialReport(OWNER_ID, MEMBER_ID, PeriodType.DAILY);

            assertThat(result.cashFlowChart()).isEmpty();
            assertThat(result.averageCashFlowChart()).isEmpty();
        }
    }
}