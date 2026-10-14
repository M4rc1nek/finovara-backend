package com.finovara.financeservice.internal.report.sharedaccount.financial;

import com.finovara.contracts.exception.badrequest.InvalidInputException;
import com.finovara.contracts.mainaccount.report.financial.dto.*;
import com.finovara.contracts.util.PeriodType;
import com.finovara.contracts.util.calculate.CalculatePercentage;
import com.finovara.contracts.util.model.ExpenseCategory;
import com.finovara.contracts.util.model.RevenueCategory;
import com.finovara.financeservice.internal.report.sharedaccount.InternalSharedReportDataService;
import com.finovara.financeservice.internal.report.util.CashFlowChartBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SharedAccountFinancialReportServiceTest {

    private static final Long OWNER_ID = 1L;
    private static final Long MEMBER_ID = 2L;
    private static final int EXPENSES_PAGE_SIZE = 5;
    private static final int REVENUES_PAGE_SIZE = 3;
    private static final LocalDate TODAY = LocalDate.now();

    private static final List<DailyCashDto> EXPENSES_BY_DATE = List.of(
            new DailyCashDto(TODAY, new BigDecimal("100.00")),
            new DailyCashDto(TODAY.minusYears(5), new BigDecimal("50.00")),
            new DailyCashDto(TODAY.plusDays(1), new BigDecimal("25.00")));
    private static final List<DailyCashDto> REVENUES_BY_DATE = List.of(
            new DailyCashDto(TODAY, new BigDecimal("300.00")),
            new DailyCashDto(TODAY.minusYears(5), new BigDecimal("20.00")),
            new DailyCashDto(TODAY.plusDays(1), new BigDecimal("5.00")));
    private static final List<DailyCashDto> EXPENSES_AVG_BY_DATE = List.of(new DailyCashDto(TODAY, new BigDecimal("11.00")));
    private static final List<DailyCashDto> REVENUES_AVG_BY_DATE = List.of(new DailyCashDto(TODAY, new BigDecimal("22.00")));

    private static final FinancialTotalsDto DEFAULT_TOTALS = new FinancialTotalsDto(
            new BigDecimal("175.00"), new BigDecimal("325.00"), new BigDecimal("150.00"));
    private static final FinancialPeriodSummaryDto DEFAULT_SUMMARY = new FinancialPeriodSummaryDto(
            new ReportDto(PeriodType.DAILY, new BigDecimal("100.00")),
            new ReportDto(PeriodType.WEEKLY, new BigDecimal("100.00")),
            new ReportDto(PeriodType.MONTHLY, new BigDecimal("100.00")),
            new ReportDto(PeriodType.DAILY, new BigDecimal("300.00")),
            new ReportDto(PeriodType.WEEKLY, new BigDecimal("300.00")),
            new ReportDto(PeriodType.MONTHLY, new BigDecimal("300.00")));
    private static final FinancialPeriodSummaryDto ZERO_SUMMARY = new FinancialPeriodSummaryDto(
            new ReportDto(PeriodType.DAILY, BigDecimal.ZERO),
            new ReportDto(PeriodType.WEEKLY, BigDecimal.ZERO),
            new ReportDto(PeriodType.MONTHLY, BigDecimal.ZERO),
            new ReportDto(PeriodType.DAILY, BigDecimal.ZERO),
            new ReportDto(PeriodType.WEEKLY, BigDecimal.ZERO),
            new ReportDto(PeriodType.MONTHLY, BigDecimal.ZERO));

    private static final ExpenseCategory EXPENSE_CATEGORY = ExpenseCategory.values()[0];
    private static final RevenueCategory REVENUE_CATEGORY = RevenueCategory.values()[0];
    private static final Map<ExpenseCategory, BigDecimal> EXPENSE_SUMS = Map.of(EXPENSE_CATEGORY, new BigDecimal("40.00"));
    private static final Map<RevenueCategory, BigDecimal> REVENUE_SUMS = Map.of(REVENUE_CATEGORY, new BigDecimal("60.00"));
    private static final List<FinancialCategoryPercentageDto> EXPECTED_EXPENSE_PERCENTAGES = Arrays.stream(ExpenseCategory.values())
            .map(category -> new FinancialCategoryPercentageDto(
                    category.name(),
                    CalculatePercentage.calculatePercentage(EXPENSE_SUMS.getOrDefault(category, BigDecimal.ZERO), new BigDecimal("40.00"))))
            .toList();
    private static final List<FinancialCategoryPercentageDto> EXPECTED_REVENUE_PERCENTAGES = Arrays.stream(RevenueCategory.values())
            .map(category -> new FinancialCategoryPercentageDto(
                    category.name(),
                    CalculatePercentage.calculatePercentage(REVENUE_SUMS.getOrDefault(category, BigDecimal.ZERO), new BigDecimal("60.00"))))
            .toList();

    @Mock
    private InternalSharedReportDataService internalSharedReportDataService;

    @Mock
    private CashFlowChartBuilder cashFlowChartBuilder;

    @Mock
    private HighestExpenseDto highestExpense;

    @Mock
    private HighestRevenueDto highestRevenue;

    @InjectMocks
    private SharedAccountFinancialReportService service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "highestExpensesPageSize", EXPENSES_PAGE_SIZE);
        ReflectionTestUtils.setField(service, "highestRevenuesPageSize", REVENUES_PAGE_SIZE);
    }

    @Nested
    class BuildReport {

        private List<HighestExpenseDto> highestExpenses;
        private List<HighestRevenueDto> highestRevenues;

        @BeforeEach
        void setUp() {
            highestExpenses = List.of(highestExpense);
            highestRevenues = List.of(highestRevenue);
            when(internalSharedReportDataService.expensesGroupedByDate(OWNER_ID, MEMBER_ID)).thenReturn(EXPENSES_BY_DATE);
            when(internalSharedReportDataService.revenuesGroupedByDate(OWNER_ID, MEMBER_ID)).thenReturn(REVENUES_BY_DATE);
            when(internalSharedReportDataService.expensesAvgGroupedByDate(OWNER_ID, MEMBER_ID)).thenReturn(EXPENSES_AVG_BY_DATE);
            when(internalSharedReportDataService.revenuesAvgGroupedByDate(OWNER_ID, MEMBER_ID)).thenReturn(REVENUES_AVG_BY_DATE);
            when(internalSharedReportDataService.highestExpenses(eq(OWNER_ID), eq(MEMBER_ID), any(LocalDate.class), any(LocalDate.class), eq(EXPENSES_PAGE_SIZE)))
                    .thenReturn(highestExpenses);
            when(internalSharedReportDataService.highestRevenues(eq(OWNER_ID), eq(MEMBER_ID), any(LocalDate.class), any(LocalDate.class), eq(REVENUES_PAGE_SIZE)))
                    .thenReturn(highestRevenues);
            when(internalSharedReportDataService.expenseSumsByCategory(eq(OWNER_ID), eq(MEMBER_ID), any(LocalDate.class), any(LocalDate.class)))
                    .thenReturn(EXPENSE_SUMS);
            when(internalSharedReportDataService.revenueSumsByCategory(eq(OWNER_ID), eq(MEMBER_ID), any(LocalDate.class), any(LocalDate.class)))
                    .thenReturn(REVENUE_SUMS);
        }

        private FinancialReportDto expectedReport(PeriodType periodType, FinancialTotalsDto totals, FinancialPeriodSummaryDto summary) {
            return new FinancialReportDto(
                    periodType,
                    totals,
                    summary,
                    highestExpenses,
                    highestRevenues,
                    EXPECTED_EXPENSE_PERCENTAGES,
                    EXPECTED_REVENUE_PERCENTAGES,
                    cashFlowChartBuilder.build(emptyList(), emptyList(), LocalDate.MIN),
                    cashFlowChartBuilder.build(emptyList(), emptyList(), LocalDate.MIN));
        }

        @ParameterizedTest
        @EnumSource(PeriodType.class)
        void shouldReturnFullReportWhenPeriodTypeIsValid(PeriodType periodType) {
            FinancialReportDto expected = expectedReport(periodType, DEFAULT_TOTALS, DEFAULT_SUMMARY);

            FinancialReportDto result = service.buildReport(OWNER_ID, MEMBER_ID, periodType);

            assertEquals(expected, result);
        }

        @Test
        void shouldReturnZeroTotalsAndPeriodSumsWhenThereAreNoEntries() {
            when(internalSharedReportDataService.expensesGroupedByDate(OWNER_ID, MEMBER_ID)).thenReturn(emptyList());
            when(internalSharedReportDataService.revenuesGroupedByDate(OWNER_ID, MEMBER_ID)).thenReturn(emptyList());
            FinancialTotalsDto totals = new FinancialTotalsDto(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
            FinancialReportDto expected = expectedReport(PeriodType.DAILY, totals, ZERO_SUMMARY);

            FinancialReportDto result = service.buildReport(OWNER_ID, MEMBER_ID, PeriodType.DAILY);

            assertEquals(expected, result);
        }

        @Test
        void shouldReturnNegativeBalanceWhenExpensesExceedRevenues() {
            when(internalSharedReportDataService.expensesGroupedByDate(OWNER_ID, MEMBER_ID))
                    .thenReturn(List.of(new DailyCashDto(TODAY, new BigDecimal("100.00"))));
            when(internalSharedReportDataService.revenuesGroupedByDate(OWNER_ID, MEMBER_ID))
                    .thenReturn(List.of(new DailyCashDto(TODAY, new BigDecimal("10.00"))));
            FinancialTotalsDto totals = new FinancialTotalsDto(
                    new BigDecimal("100.00"), new BigDecimal("10.00"), new BigDecimal("-90.00"));
            FinancialPeriodSummaryDto summary = new FinancialPeriodSummaryDto(
                    new ReportDto(PeriodType.DAILY, new BigDecimal("100.00")),
                    new ReportDto(PeriodType.WEEKLY, new BigDecimal("100.00")),
                    new ReportDto(PeriodType.MONTHLY, new BigDecimal("100.00")),
                    new ReportDto(PeriodType.DAILY, new BigDecimal("10.00")),
                    new ReportDto(PeriodType.WEEKLY, new BigDecimal("10.00")),
                    new ReportDto(PeriodType.MONTHLY, new BigDecimal("10.00")));
            FinancialReportDto expected = expectedReport(PeriodType.MONTHLY, totals, summary);

            FinancialReportDto result = service.buildReport(OWNER_ID, MEMBER_ID, PeriodType.MONTHLY);

            assertEquals(expected, result);
        }

        @Test
        void shouldReturnZeroPeriodSumsWhenEntriesAreOutsideAllPeriods() {
            when(internalSharedReportDataService.expensesGroupedByDate(OWNER_ID, MEMBER_ID)).thenReturn(List.of(
                    new DailyCashDto(TODAY.minusYears(5), new BigDecimal("50.00")),
                    new DailyCashDto(TODAY.plusDays(1), new BigDecimal("25.00"))));
            when(internalSharedReportDataService.revenuesGroupedByDate(OWNER_ID, MEMBER_ID)).thenReturn(List.of(
                    new DailyCashDto(TODAY.minusYears(5), new BigDecimal("20.00")),
                    new DailyCashDto(TODAY.plusDays(1), new BigDecimal("5.00"))));
            FinancialTotalsDto totals = new FinancialTotalsDto(
                    new BigDecimal("75.00"), new BigDecimal("25.00"), new BigDecimal("-50.00"));
            FinancialReportDto expected = expectedReport(PeriodType.WEEKLY, totals, ZERO_SUMMARY);

            FinancialReportDto result = service.buildReport(OWNER_ID, MEMBER_ID, PeriodType.WEEKLY);

            assertEquals(expected, result);
        }

        @ParameterizedTest
        @EnumSource(PeriodType.class)
        void shouldQueryHighestEntriesWithPeriodStartAndConfiguredPageSizesWhenPeriodTypeIsGiven(PeriodType periodType) {
            LocalDate from = periodType.getStartDate(TODAY);

            service.buildReport(OWNER_ID, MEMBER_ID, periodType);

            verify(internalSharedReportDataService).highestExpenses(OWNER_ID, MEMBER_ID, from, TODAY, EXPENSES_PAGE_SIZE);
            verify(internalSharedReportDataService).highestRevenues(OWNER_ID, MEMBER_ID, from, TODAY, REVENUES_PAGE_SIZE);
        }

        @ParameterizedTest
        @EnumSource(PeriodType.class)
        void shouldQueryCategorySumsWithPeriodRangeWhenPeriodTypeIsGiven(PeriodType periodType) {
            LocalDate from = periodType.getStartDate(TODAY);

            service.buildReport(OWNER_ID, MEMBER_ID, periodType);

            verify(internalSharedReportDataService).expenseSumsByCategory(OWNER_ID, MEMBER_ID, from, TODAY);
            verify(internalSharedReportDataService).revenueSumsByCategory(OWNER_ID, MEMBER_ID, from, TODAY);
        }

        @Test
        void shouldBuildCashFlowChartsFromSumAndAverageDataWhenReportIsBuilt() {
            service.buildReport(OWNER_ID, MEMBER_ID, PeriodType.DAILY);

            verify(cashFlowChartBuilder).build(EXPENSES_BY_DATE, REVENUES_BY_DATE, TODAY);
            verify(cashFlowChartBuilder).build(EXPENSES_AVG_BY_DATE, REVENUES_AVG_BY_DATE, TODAY);
        }

        @Test
        void shouldFetchDataOnlyOnceWhenReportIsBuilt() {
            LocalDate from = PeriodType.DAILY.getStartDate(TODAY);

            service.buildReport(OWNER_ID, MEMBER_ID, PeriodType.DAILY);

            verify(internalSharedReportDataService, times(1)).expensesGroupedByDate(OWNER_ID, MEMBER_ID);
            verify(internalSharedReportDataService, times(1)).revenuesGroupedByDate(OWNER_ID, MEMBER_ID);
            verify(internalSharedReportDataService, times(1)).expensesAvgGroupedByDate(OWNER_ID, MEMBER_ID);
            verify(internalSharedReportDataService, times(1)).revenuesAvgGroupedByDate(OWNER_ID, MEMBER_ID);
            verify(internalSharedReportDataService, times(1)).highestExpenses(OWNER_ID, MEMBER_ID, from, TODAY, EXPENSES_PAGE_SIZE);
            verify(internalSharedReportDataService, times(1)).highestRevenues(OWNER_ID, MEMBER_ID, from, TODAY, REVENUES_PAGE_SIZE);
            verify(internalSharedReportDataService, times(1)).expenseSumsByCategory(OWNER_ID, MEMBER_ID, from, TODAY);
            verify(internalSharedReportDataService, times(1)).revenueSumsByCategory(OWNER_ID, MEMBER_ID, from, TODAY);
            verifyNoMoreInteractions(internalSharedReportDataService);
        }
    }

    @Nested
    class BuildReportWithInvalidInput {

        @Test
        void shouldThrowExceptionWhenPeriodTypeIsNull() {
            assertThrows(InvalidInputException.class, () -> service.buildReport(OWNER_ID, MEMBER_ID, null));
        }

        @Test
        void shouldThrowExceptionWhenPeriodTypeIsNullAndNotInteractWithDependencies() {
            assertThrows(InvalidInputException.class, () -> service.buildReport(OWNER_ID, MEMBER_ID, null));

            verifyNoInteractions(internalSharedReportDataService, cashFlowChartBuilder);
        }
    }
}