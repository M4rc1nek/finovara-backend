package com.finovara.reportservice.pdfexport.service.strategy;

import com.finovara.contracts.model.PeriodType;
import com.finovara.contracts.model.transaction.ExpenseCategory;
import com.finovara.contracts.model.transaction.RevenueCategory;
import com.finovara.reportservice.feignclient.CoreBackendReportClient;
import com.finovara.reportservice.pdfexport.document.PdfReportDocument;
import com.finovara.reportservice.pdfexport.model.PdfReportType;
import com.finovara.reportservice.report.dto.ReportDto;
import com.finovara.reportservice.report.finances.average.service.ReportAverageService;
import com.finovara.reportservice.report.finances.categorypercentage.expense.dto.ExpenseCategoryPercentageDto;
import com.finovara.reportservice.report.finances.categorypercentage.expense.service.ExpenseCategoryPercentageService;
import com.finovara.reportservice.report.finances.categorypercentage.revenue.dto.RevenueCategoryPercentageDto;
import com.finovara.reportservice.report.finances.categorypercentage.revenue.service.RevenueCategoryPercentageService;
import com.finovara.reportservice.report.finances.sum.service.ReportSummaryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ImportantInformationPdfTest {

    private static final Long USER_ID = 1L;
    private static final PeriodType PERIOD_TYPE = PeriodType.DAILY;
    private static final BigDecimal WALLET_BALANCE = BigDecimal.valueOf(5000);
    private static final BigDecimal MONTHLY_REVENUE = BigDecimal.valueOf(3000);
    private static final BigDecimal MONTHLY_EXPENSE = BigDecimal.valueOf(1500);
    private static final BigDecimal WEEKLY_REVENUE = BigDecimal.valueOf(750);
    private static final BigDecimal WEEKLY_EXPENSE = BigDecimal.valueOf(300);
    private static final BigDecimal DAILY_AVG_REVENUE = BigDecimal.valueOf(100);
    private static final BigDecimal DAILY_AVG_EXPENSE = BigDecimal.valueOf(50);
    private static final BigDecimal ALL_REVENUES = BigDecimal.valueOf(20000);
    private static final BigDecimal ALL_EXPENSES = BigDecimal.valueOf(10000);

    @Mock
    private CoreBackendReportClient reportClient;
    @Mock
    private ReportSummaryService reportSummaryService;
    @Mock
    private ReportAverageService reportAverageService;
    @Mock
    private ExpenseCategoryPercentageService expenseCategoryPercentageService;
    @Mock
    private RevenueCategoryPercentageService revenueCategoryPercentageService;
    @Mock
    private PdfReportDocument document;

    private ImportantInformationPdf importantInformationPdf;

    @BeforeEach
    void setUp() {
        importantInformationPdf = new ImportantInformationPdf(
                reportClient,
                reportSummaryService,
                reportAverageService,
                expenseCategoryPercentageService,
                revenueCategoryPercentageService
        );
    }

    @Nested
    class GetType {

        @Test
        void shouldReturnImportantInformationType() {
            assertThat(importantInformationPdf.getType()).isEqualTo(PdfReportType.IMPORTANT_INFORMATION);
        }
    }

    @Nested
    class GetTitle {

        @ParameterizedTest
        @EnumSource(PeriodType.class)
        void shouldReturnConstantTitleRegardlessOfPeriod(PeriodType periodType) {
            assertThat(importantInformationPdf.getTitle(periodType)).isEqualTo("Najważniejsze informacje");
        }
    }

    @Nested
    class GetFileName {

        @ParameterizedTest
        @EnumSource(PeriodType.class)
        void shouldReturnConstantFileNameRegardlessOfPeriod(PeriodType periodType) {
            assertThat(importantInformationPdf.getFileName(periodType)).isEqualTo("najwazniejsze-informacje.pdf");
        }
    }

    @Nested
    class Generate {

        @BeforeEach
        void stubDefaultDependencies() {
            when(reportClient.walletBalance(USER_ID)).thenReturn(WALLET_BALANCE);
            when(reportClient.sumAllExpenses(USER_ID)).thenReturn(ALL_EXPENSES);
            when(reportClient.sumAllRevenues(USER_ID)).thenReturn(ALL_REVENUES);

            when(reportSummaryService.sumRevenue(USER_ID, PeriodType.MONTHLY))
                    .thenReturn(new ReportDto(PeriodType.MONTHLY, MONTHLY_REVENUE));
            when(reportSummaryService.sumExpense(USER_ID, PeriodType.MONTHLY))
                    .thenReturn(new ReportDto(PeriodType.MONTHLY, MONTHLY_EXPENSE));
            when(reportSummaryService.sumRevenue(USER_ID, PeriodType.WEEKLY))
                    .thenReturn(new ReportDto(PeriodType.WEEKLY, WEEKLY_REVENUE));
            when(reportSummaryService.sumExpense(USER_ID, PeriodType.WEEKLY))
                    .thenReturn(new ReportDto(PeriodType.WEEKLY, WEEKLY_EXPENSE));

            when(reportAverageService.calculateAverageRevenue(USER_ID, PeriodType.DAILY))
                    .thenReturn(new ReportDto(PeriodType.DAILY, DAILY_AVG_REVENUE));
            when(reportAverageService.calculateAverageExpense(USER_ID, PeriodType.DAILY))
                    .thenReturn(new ReportDto(PeriodType.DAILY, DAILY_AVG_EXPENSE));

            when(expenseCategoryPercentageService.getExpensePercentageByCategoryReport(
                    eq(USER_ID), any(ExpenseCategory.class), eq(PeriodType.MONTHLY)))
                    .thenReturn(new ExpenseCategoryPercentageDto(BigDecimal.valueOf(10), ExpenseCategory.FOOD));

            when(revenueCategoryPercentageService.getRevenuePercentageByCategoryReport(
                    eq(USER_ID), any(RevenueCategory.class), eq(PeriodType.MONTHLY)))
                    .thenReturn(new RevenueCategoryPercentageDto(BigDecimal.valueOf(10), RevenueCategory.SALARY));

            when(document.formatMoney(any())).thenAnswer(invocation -> invocation.getArgument(0) + " PLN");
            when(document.formatPercent(any())).thenAnswer(invocation -> invocation.getArgument(0) + "%");
        }

        @Test
        void shouldAddSectionsInOrder() throws IOException {
            importantInformationPdf.generate(document, USER_ID, PERIOD_TYPE);

            InOrder inOrder = inOrder(document);
            inOrder.verify(document).addSection("Podstawowe informacje");
            inOrder.verify(document).addSection("Szczegółowe informacje");
        }

        @Test
        void shouldAddBarChartWithMonthlyAndWeeklyTotals() throws IOException {
            importantInformationPdf.generate(document, USER_ID, PERIOD_TYPE);

            verify(document).addBarChart(
                    eq("Przychody i wydatki w aktualnym okresie"),
                    eq(List.of("Mies. przychody", "Mies. wydatki", "Tyg. przychody", "Tyg. wydatki")),
                    eq(List.of(MONTHLY_REVENUE, MONTHLY_EXPENSE, WEEKLY_REVENUE, WEEKLY_EXPENSE)),
                    eq(true)
            );
        }

        @Test
        void shouldAddPieChartAndTwoTables() throws IOException {
            importantInformationPdf.generate(document, USER_ID, PERIOD_TYPE);

            verify(document).addPieChart(eq("Struktura wydatków w tym miesiącu"), any(), any());
            verify(document, times(2)).addTable(any(), any());
        }

        @Test
        void shouldFormatSpentAndSavedPercentagesSymmetrically() throws IOException {
            importantInformationPdf.generate(document, USER_ID, PERIOD_TYPE);

            verify(document, times(2)).formatPercent(new BigDecimal("50.00"));
        }

        @Test
        void shouldReturnZeroSpentPercentageWhenAllRevenuesAreZero() throws IOException {
            when(reportClient.sumAllRevenues(USER_ID)).thenReturn(BigDecimal.ZERO);

            importantInformationPdf.generate(document, USER_ID, PERIOD_TYPE);

            verify(document).formatPercent(BigDecimal.ZERO);
        }

        @Test
        void shouldClampSavedPercentageToZeroWhenExpensesExceedRevenues() throws IOException {
            when(reportClient.sumAllExpenses(USER_ID)).thenReturn(BigDecimal.valueOf(30000));
            when(reportClient.sumAllRevenues(USER_ID)).thenReturn(BigDecimal.valueOf(10000));

            importantInformationPdf.generate(document, USER_ID, PERIOD_TYPE);

            verify(document).formatPercent(BigDecimal.ZERO);
        }

        @ParameterizedTest
        @EnumSource(PeriodType.class)
        void shouldIgnoreRequestedPeriodType(PeriodType periodType) throws IOException {
            importantInformationPdf.generate(document, USER_ID, periodType);

            verify(reportSummaryService).sumRevenue(USER_ID, PeriodType.MONTHLY);
            verify(reportSummaryService).sumExpense(USER_ID, PeriodType.MONTHLY);
            verify(reportSummaryService).sumRevenue(USER_ID, PeriodType.WEEKLY);
            verify(reportSummaryService).sumExpense(USER_ID, PeriodType.WEEKLY);
            verify(reportAverageService).calculateAverageRevenue(USER_ID, PeriodType.DAILY);
            verify(reportAverageService).calculateAverageExpense(USER_ID, PeriodType.DAILY);
        }
    }
}
