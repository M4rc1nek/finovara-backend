package com.finovara.reportservice.pdfexport.strategy;

import com.finovara.contracts.mainaccount.report.financial.dto.FinancialPeriodSummaryDto;
import com.finovara.contracts.mainaccount.report.financial.dto.FinancialReportDto;
import com.finovara.contracts.mainaccount.report.financial.dto.FinancialTotalsDto;
import com.finovara.contracts.mainaccount.report.financial.dto.ReportDto;
import com.finovara.contracts.util.PeriodType;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PdfReportValuesTest {

    private static final BigDecimal AMOUNT = new BigDecimal("123.45");
    private static final BigDecimal TOTAL = new BigDecimal("1000.00");

    @Mock
    private FinancialReportDto report;

    @Mock
    private FinancialTotalsDto totals;

    @Mock
    private FinancialPeriodSummaryDto summary;

    @Mock
    private ReportDto reportDto;

    @Nested
    class Revenues {

        @Test
        void shouldReturnRevenuesWhenTotalsExist() {
            when(report.totals()).thenReturn(totals);
            when(totals.revenues()).thenReturn(AMOUNT);

            assertEquals(AMOUNT, PdfReportValues.revenues(report));
        }

        @Test
        void shouldReturnZeroWhenTotalsAreNull() {
            when(report.totals()).thenReturn(null);

            assertEquals(BigDecimal.ZERO, PdfReportValues.revenues(report));
        }

        @Test
        void shouldReturnZeroWhenRevenuesAreNull() {
            when(report.totals()).thenReturn(totals);
            when(totals.revenues()).thenReturn(null);

            assertEquals(BigDecimal.ZERO, PdfReportValues.revenues(report));
        }

        @Test
        void shouldThrowExceptionWhenReportIsNull() {
            assertThrows(NullPointerException.class, () -> PdfReportValues.revenues(null));
        }
    }

    @Nested
    class Expenses {

        @Test
        void shouldReturnExpensesWhenTotalsExist() {
            when(report.totals()).thenReturn(totals);
            when(totals.expenses()).thenReturn(AMOUNT);

            assertEquals(AMOUNT, PdfReportValues.expenses(report));
        }

        @Test
        void shouldReturnZeroWhenTotalsAreNull() {
            when(report.totals()).thenReturn(null);

            assertEquals(BigDecimal.ZERO, PdfReportValues.expenses(report));
        }

        @Test
        void shouldReturnZeroWhenExpensesAreNull() {
            when(report.totals()).thenReturn(totals);
            when(totals.expenses()).thenReturn(null);

            assertEquals(BigDecimal.ZERO, PdfReportValues.expenses(report));
        }

        @Test
        void shouldThrowExceptionWhenReportIsNull() {
            assertThrows(NullPointerException.class, () -> PdfReportValues.expenses(null));
        }
    }

    @Nested
    class RevenueFor {

        @Test
        void shouldReturnDailyRevenueWhenPeriodIsDaily() {
            when(report.periodSummary()).thenReturn(summary);
            when(summary.dailyRevenue()).thenReturn(reportDto);
            when(reportDto.amount()).thenReturn(AMOUNT);

            assertEquals(AMOUNT, PdfReportValues.revenueFor(report, PeriodType.DAILY));
        }

        @Test
        void shouldReturnWeeklyRevenueWhenPeriodIsWeekly() {
            when(report.periodSummary()).thenReturn(summary);
            when(summary.weeklyRevenue()).thenReturn(reportDto);
            when(reportDto.amount()).thenReturn(AMOUNT);

            assertEquals(AMOUNT, PdfReportValues.revenueFor(report, PeriodType.WEEKLY));
        }

        @Test
        void shouldReturnMonthlyRevenueWhenPeriodIsMonthly() {
            when(report.periodSummary()).thenReturn(summary);
            when(summary.monthlyRevenue()).thenReturn(reportDto);
            when(reportDto.amount()).thenReturn(AMOUNT);

            assertEquals(AMOUNT, PdfReportValues.revenueFor(report, PeriodType.MONTHLY));
        }

        @Test
        void shouldReturnZeroWhenPeriodSummaryIsNull() {
            when(report.periodSummary()).thenReturn(null);

            assertEquals(BigDecimal.ZERO, PdfReportValues.revenueFor(report, PeriodType.MONTHLY));
        }

        @Test
        void shouldReturnZeroWhenSelectedReportIsNull() {
            when(report.periodSummary()).thenReturn(summary);
            when(summary.monthlyRevenue()).thenReturn(null);

            assertEquals(BigDecimal.ZERO, PdfReportValues.revenueFor(report, PeriodType.MONTHLY));
        }

        @Test
        void shouldReturnZeroWhenAmountIsNull() {
            when(report.periodSummary()).thenReturn(summary);
            when(summary.monthlyRevenue()).thenReturn(reportDto);
            when(reportDto.amount()).thenReturn(null);

            assertEquals(BigDecimal.ZERO, PdfReportValues.revenueFor(report, PeriodType.MONTHLY));
        }

        @Test
        void shouldThrowExceptionWhenPeriodIsNullAndSummaryExists() {
            when(report.periodSummary()).thenReturn(summary);

            assertThrows(NullPointerException.class, () -> PdfReportValues.revenueFor(report, null));
        }

        @Test
        void shouldThrowExceptionWhenReportIsNull() {
            assertThrows(NullPointerException.class,
                    () -> PdfReportValues.revenueFor(null, PeriodType.MONTHLY));
        }
    }

    @Nested
    class ExpenseFor {

        @Test
        void shouldReturnDailyExpenseWhenPeriodIsDaily() {
            when(report.periodSummary()).thenReturn(summary);
            when(summary.dailyExpense()).thenReturn(reportDto);
            when(reportDto.amount()).thenReturn(AMOUNT);

            assertEquals(AMOUNT, PdfReportValues.expenseFor(report, PeriodType.DAILY));
        }

        @Test
        void shouldReturnWeeklyExpenseWhenPeriodIsWeekly() {
            when(report.periodSummary()).thenReturn(summary);
            when(summary.weeklyExpense()).thenReturn(reportDto);
            when(reportDto.amount()).thenReturn(AMOUNT);

            assertEquals(AMOUNT, PdfReportValues.expenseFor(report, PeriodType.WEEKLY));
        }

        @Test
        void shouldReturnMonthlyExpenseWhenPeriodIsMonthly() {
            when(report.periodSummary()).thenReturn(summary);
            when(summary.monthlyExpense()).thenReturn(reportDto);
            when(reportDto.amount()).thenReturn(AMOUNT);

            assertEquals(AMOUNT, PdfReportValues.expenseFor(report, PeriodType.MONTHLY));
        }

        @Test
        void shouldReturnZeroWhenPeriodSummaryIsNull() {
            when(report.periodSummary()).thenReturn(null);

            assertEquals(BigDecimal.ZERO, PdfReportValues.expenseFor(report, PeriodType.MONTHLY));
        }

        @Test
        void shouldReturnZeroWhenSelectedReportIsNull() {
            when(report.periodSummary()).thenReturn(summary);
            when(summary.monthlyExpense()).thenReturn(null);

            assertEquals(BigDecimal.ZERO, PdfReportValues.expenseFor(report, PeriodType.MONTHLY));
        }

        @Test
        void shouldReturnZeroWhenAmountIsNull() {
            when(report.periodSummary()).thenReturn(summary);
            when(summary.monthlyExpense()).thenReturn(reportDto);
            when(reportDto.amount()).thenReturn(null);

            assertEquals(BigDecimal.ZERO, PdfReportValues.expenseFor(report, PeriodType.MONTHLY));
        }

        @Test
        void shouldThrowExceptionWhenPeriodIsNullAndSummaryExists() {
            when(report.periodSummary()).thenReturn(summary);

            assertThrows(NullPointerException.class, () -> PdfReportValues.expenseFor(report, null));
        }

        @Test
        void shouldThrowExceptionWhenReportIsNull() {
            assertThrows(NullPointerException.class,
                    () -> PdfReportValues.expenseFor(null, PeriodType.MONTHLY));
        }
    }

    @Nested
    class AveragePerDay {

        @ParameterizedTest
        @EnumSource(PeriodType.class)
        void shouldDivideTotalByNumberOfDaysSincePeriodStartWhenTotalIsPositive(PeriodType periodType) {
            LocalDate today = LocalDate.now();
            long days = ChronoUnit.DAYS.between(periodType.getStartDate(today), today) + 1;
            BigDecimal expected = TOTAL.divide(BigDecimal.valueOf(days), 2, RoundingMode.HALF_UP);

            assertEquals(expected, PdfReportValues.averagePerDay(TOTAL, periodType));
        }

        @ParameterizedTest
        @EnumSource(PeriodType.class)
        void shouldReturnZeroWithScaleTwoWhenTotalIsZero(PeriodType periodType) {
            assertEquals(new BigDecimal("0.00"), PdfReportValues.averagePerDay(BigDecimal.ZERO, periodType));
        }

        @ParameterizedTest
        @EnumSource(PeriodType.class)
        void shouldReturnNegativeAverageWhenTotalIsNegative(PeriodType periodType) {
            BigDecimal result = PdfReportValues.averagePerDay(TOTAL.negate(), periodType);

            assertTrue(result.signum() < 0);
        }

        @ParameterizedTest
        @EnumSource(PeriodType.class)
        void shouldReturnResultWithScaleTwoWhenTotalHasDifferentScale(PeriodType periodType) {
            BigDecimal result = PdfReportValues.averagePerDay(new BigDecimal("100"), periodType);

            assertEquals(2, result.scale());
        }

        @Test
        void shouldReturnTotalWhenPeriodIsDaily() {
            assertEquals(new BigDecimal("1000.00"), PdfReportValues.averagePerDay(TOTAL, PeriodType.DAILY));
        }

        @Test
        void shouldThrowExceptionWhenTotalIsNull() {
            assertThrows(NullPointerException.class,
                    () -> PdfReportValues.averagePerDay(null, PeriodType.MONTHLY));
        }

        @Test
        void shouldThrowExceptionWhenPeriodIsNull() {
            assertThrows(NullPointerException.class, () -> PdfReportValues.averagePerDay(TOTAL, null));
        }
    }

    @Nested
    class Safe {

        @Test
        void shouldReturnEmptyListWhenListIsNull() {
            List<String> result = PdfReportValues.safe(null);

            assertTrue(result.isEmpty());
        }

        @Test
        void shouldReturnSameListWhenListIsNotNull() {
            List<String> list = new ArrayList<>(List.of("a", "b"));

            assertSame(list, PdfReportValues.safe(list));
        }

        @Test
        void shouldReturnEmptyListWhenListIsEmpty() {
            List<String> list = List.of();

            assertSame(list, PdfReportValues.safe(list));
        }
    }
}