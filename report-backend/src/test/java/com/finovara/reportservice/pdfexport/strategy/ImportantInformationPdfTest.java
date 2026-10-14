package com.finovara.reportservice.pdfexport.strategy;

import com.finovara.contracts.mainaccount.report.financial.dto.FinancialCategoryPercentageDto;
import com.finovara.contracts.mainaccount.report.financial.dto.FinancialPeriodSummaryDto;
import com.finovara.contracts.mainaccount.report.financial.dto.FinancialReportDto;
import com.finovara.contracts.mainaccount.report.financial.dto.ReportDto;
import com.finovara.contracts.util.PeriodType;
import com.finovara.reportservice.pdfexport.PdfReportContext;
import com.finovara.reportservice.pdfexport.document.PdfReportDocument;
import com.finovara.reportservice.pdfexport.model.PdfReportType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ImportantInformationPdfTest {

    private static final String NO_DATA = "Brak danych";
    private static final String[] HEADERS = {"Informacja", "Wartość"};
    private static final BigDecimal WALLET_BALANCE = new BigDecimal("5000.00");
    private static final BigDecimal MONTHLY_REVENUE = new BigDecimal("1000.00");
    private static final BigDecimal MONTHLY_EXPENSE = new BigDecimal("250.00");
    private static final BigDecimal WEEKLY_REVENUE = new BigDecimal("300.00");
    private static final BigDecimal WEEKLY_EXPENSE = new BigDecimal("100.00");
    private static final BigDecimal AVERAGE_REVENUE = PdfReportValues.averagePerDay(MONTHLY_REVENUE, PeriodType.MONTHLY);
    private static final BigDecimal AVERAGE_EXPENSE = PdfReportValues.averagePerDay(MONTHLY_EXPENSE, PeriodType.MONTHLY);
    private static final BigDecimal FOOD_PERCENTAGE = new BigDecimal("40.00");
    private static final BigDecimal TRANSPORT_PERCENTAGE = new BigDecimal("10.00");
    private static final BigDecimal HOUSING_PERCENTAGE = new BigDecimal("50.00");
    private static final BigDecimal ZERO_PERCENTAGE = new BigDecimal("0.00");
    private static final BigDecimal SALARY_PERCENTAGE = new BigDecimal("60.00");
    private static final BigDecimal BUSINESS_PERCENTAGE = new BigDecimal("40.00");

    @Mock
    private PdfReportDocument document;

    @Mock
    private PdfReportContext context;

    @Mock
    private FinancialReportDto report;

    @Mock
    private FinancialPeriodSummaryDto summary;

    @Mock
    private ReportDto monthlyRevenueDto;

    @Mock
    private ReportDto monthlyExpenseDto;

    @Mock
    private ReportDto weeklyRevenueDto;

    @Mock
    private ReportDto weeklyExpenseDto;

    @Mock
    private FinancialCategoryPercentageDto foodExpense;

    @Mock
    private FinancialCategoryPercentageDto transportExpense;

    @Mock
    private FinancialCategoryPercentageDto housingExpense;

    @Mock
    private FinancialCategoryPercentageDto zeroExpense;

    @Mock
    private FinancialCategoryPercentageDto salaryRevenue;

    @Mock
    private FinancialCategoryPercentageDto businessRevenue;

    @Captor
    private ArgumentCaptor<String[]> headersCaptor;

    @Captor
    private ArgumentCaptor<List<String[]>> rowsCaptor;

    @InjectMocks
    private ImportantInformationPdf handler;

    @Nested
    class GetType {

        @Test
        void shouldReturnImportantInformationTypeWhenTypeIsRequested() {
            assertEquals(PdfReportType.IMPORTANT_INFORMATION, handler.getType());
        }
    }

    @Nested
    class GetTitle {

        @ParameterizedTest
        @EnumSource(PeriodType.class)
        void shouldReturnTitleWhenPeriodTypeIsGiven(PeriodType periodType) {
            assertEquals("Najważniejsze informacje", handler.getTitle(periodType));
        }

        @Test
        void shouldReturnTitleWhenPeriodTypeIsNull() {
            assertEquals("Najważniejsze informacje", handler.getTitle(null));
        }
    }

    @Nested
    class GetFileName {

        @ParameterizedTest
        @EnumSource(PeriodType.class)
        void shouldReturnFixedFileNameWhenPeriodTypeIsGiven(PeriodType periodType) {
            assertEquals("najwazniejsze-informacje.pdf", handler.getFileName(periodType));
        }

        @Test
        void shouldReturnFixedFileNameWhenPeriodTypeIsNull() {
            assertEquals("najwazniejsze-informacje.pdf", handler.getFileName(null));
        }
    }

    @Nested
    class Generate {

        @BeforeEach
        void setUp() {
            when(context.report(PeriodType.MONTHLY)).thenReturn(report);
            when(context.walletBalance()).thenReturn(WALLET_BALANCE);
            when(report.periodSummary()).thenReturn(summary);
            when(summary.monthlyRevenue()).thenReturn(monthlyRevenueDto);
            when(summary.monthlyExpense()).thenReturn(monthlyExpenseDto);
            when(summary.weeklyRevenue()).thenReturn(weeklyRevenueDto);
            when(summary.weeklyExpense()).thenReturn(weeklyExpenseDto);
            when(monthlyRevenueDto.amount()).thenReturn(MONTHLY_REVENUE);
            when(monthlyExpenseDto.amount()).thenReturn(MONTHLY_EXPENSE);
            when(weeklyRevenueDto.amount()).thenReturn(WEEKLY_REVENUE);
            when(weeklyExpenseDto.amount()).thenReturn(WEEKLY_EXPENSE);
        }

        @Test
        void shouldFetchMonthlyReportWhenGeneratingInformation() throws IOException {
            handler.generate(document, context);

            verify(context).report(PeriodType.MONTHLY);
            verify(context, never()).report();
        }

        @Test
        void shouldAddBothSectionsWhenReportIsGenerated() throws IOException {
            handler.generate(document, context);

            verify(document).addSection("Podstawowe informacje");
            verify(document).addSection("Szczegółowe informacje");
        }

        @Test
        void shouldAddBarChartWithPeriodValuesWhenSummaryExists() throws IOException {
            handler.generate(document, context);

            verify(document).addBarChart("Przychody i wydatki w aktualnym okresie",
                    List.of("Mies. przychody", "Mies. wydatki", "Tyg. przychody", "Tyg. wydatki"),
                    List.of(MONTHLY_REVENUE, MONTHLY_EXPENSE, WEEKLY_REVENUE, WEEKLY_EXPENSE), true);
        }

        @Test
        void shouldAddTablesWithHeadersWhenReportIsGenerated() throws IOException {
            handler.generate(document, context);

            verify(document, times(2)).addTable(headersCaptor.capture(), rowsCaptor.capture());
            assertArrayEquals(HEADERS, headersCaptor.getAllValues().get(0));
            assertArrayEquals(HEADERS, headersCaptor.getAllValues().get(1));
        }

        @Test
        void shouldAddBasicTableRowsWhenSummaryExists() throws IOException {
            when(document.formatMoney(any(BigDecimal.class))).thenAnswer(invocation -> invocation.getArgument(0).toString());

            handler.generate(document, context);

            verify(document, times(2)).addTable(headersCaptor.capture(), rowsCaptor.capture());
            List<String[]> rows = rowsCaptor.getAllValues().get(0);
            assertEquals(7, rows.size());
            assertArrayEquals(new String[]{"Saldo portfela", "5000.00"}, rows.get(0));
            assertArrayEquals(new String[]{"W tym miesiącu zarobiłeś", "1000.00"}, rows.get(1));
            assertArrayEquals(new String[]{"W tym miesiącu wydałeś", "250.00"}, rows.get(2));
            assertArrayEquals(new String[]{"W tym tygodniu zarobiłeś", "300.00"}, rows.get(3));
            assertArrayEquals(new String[]{"W tym tygodniu wydałeś", "100.00"}, rows.get(4));
            assertArrayEquals(new String[]{"Średnie dzienne przychody", AVERAGE_REVENUE + "/dzień"}, rows.get(5));
            assertArrayEquals(new String[]{"Średnie dzienne wydatki", AVERAGE_EXPENSE + "/dzień"}, rows.get(6));
        }

        @Test
        void shouldAddElementsInOrderWhenReportIsGenerated() throws IOException {
            handler.generate(document, context);

            InOrder inOrder = inOrder(document);
            inOrder.verify(document).addSection("Podstawowe informacje");
            inOrder.verify(document).addBarChart(anyString(), any(), any(), anyBoolean());
            inOrder.verify(document).addTable(any(), any());
            inOrder.verify(document).addSection("Szczegółowe informacje");
            inOrder.verify(document).addPieChart(anyString(), any(), any());
            inOrder.verify(document).addTable(any(), any());
        }

        @Test
        void shouldAddEmptyPieChartWhenExpensePercentagesAreEmpty() throws IOException {
            handler.generate(document, context);

            verify(document).addPieChart("Struktura wydatków w tym miesiącu", List.of(), List.of());
        }

        @Test
        void shouldShowNoDataWhenPercentagesAreEmpty() throws IOException {
            handler.generate(document, context);

            verify(document, times(2)).addTable(headersCaptor.capture(), rowsCaptor.capture());
            List<String[]> rows = rowsCaptor.getAllValues().get(1);
            assertEquals(6, rows.size());
            assertArrayEquals(new String[]{"Główne źródło przychodu", NO_DATA}, rows.get(2));
            assertArrayEquals(new String[]{"Największy procent wydatków stanowi", NO_DATA}, rows.get(3));
            assertArrayEquals(new String[]{"Najwięcej wydajesz na", NO_DATA}, rows.get(4));
            assertArrayEquals(new String[]{"Najmniej wydajesz na", NO_DATA}, rows.get(5));
        }

        @Test
        void shouldShowNoDataWhenPercentagesAreNull() throws IOException {
            when(report.expenseCategoryPercentages()).thenReturn(null);
            when(report.revenueCategoryPercentages()).thenReturn(null);

            handler.generate(document, context);

            verify(document, times(2)).addTable(headersCaptor.capture(), rowsCaptor.capture());
            List<String[]> rows = rowsCaptor.getAllValues().get(1);
            assertArrayEquals(new String[]{"Główne źródło przychodu", NO_DATA}, rows.get(2));
            assertArrayEquals(new String[]{"Największy procent wydatków stanowi", NO_DATA}, rows.get(3));
            assertArrayEquals(new String[]{"Najmniej wydajesz na", NO_DATA}, rows.get(5));
        }

        @Test
        void shouldShowNoDataWhenAllPercentagesAreZero() throws IOException {
            when(report.expenseCategoryPercentages()).thenReturn(List.of(zeroExpense));
            when(report.revenueCategoryPercentages()).thenReturn(List.of(salaryRevenue));
            when(zeroExpense.category()).thenReturn("OTHERS");
            when(zeroExpense.percentage()).thenReturn(ZERO_PERCENTAGE);
            when(salaryRevenue.percentage()).thenReturn(ZERO_PERCENTAGE);

            handler.generate(document, context);

            verify(document, times(2)).addTable(headersCaptor.capture(), rowsCaptor.capture());
            List<String[]> rows = rowsCaptor.getAllValues().get(1);
            assertArrayEquals(new String[]{"Główne źródło przychodu", NO_DATA}, rows.get(2));
            assertArrayEquals(new String[]{"Największy procent wydatków stanowi", NO_DATA}, rows.get(3));
            assertArrayEquals(new String[]{"Najwięcej wydajesz na", NO_DATA}, rows.get(4));
            assertArrayEquals(new String[]{"Najmniej wydajesz na", NO_DATA}, rows.get(5));
        }

        @Test
        void shouldShowSpentAndSavedPercentagesWhenRevenueAndExpenseExist() throws IOException {
            when(document.formatPercent(any(BigDecimal.class))).thenAnswer(invocation -> invocation.getArgument(0).toString());

            handler.generate(document, context);

            verify(document, times(2)).addTable(headersCaptor.capture(), rowsCaptor.capture());
            List<String[]> rows = rowsCaptor.getAllValues().get(1);
            assertArrayEquals(new String[]{"Z przychodów wydajesz", "25.00"}, rows.get(0));
            assertArrayEquals(new String[]{"Z przychodów oszczędzasz", "75.00"}, rows.get(1));
        }

        @Test
        void shouldShowZeroSpentAndFullSavedWhenMonthlyRevenueIsZero() throws IOException {
            when(monthlyRevenueDto.amount()).thenReturn(BigDecimal.ZERO);
            when(document.formatPercent(any(BigDecimal.class))).thenAnswer(invocation -> invocation.getArgument(0).toString());

            handler.generate(document, context);

            verify(document, times(2)).addTable(headersCaptor.capture(), rowsCaptor.capture());
            List<String[]> rows = rowsCaptor.getAllValues().get(1);
            assertArrayEquals(new String[]{"Z przychodów wydajesz", "0"}, rows.get(0));
            assertArrayEquals(new String[]{"Z przychodów oszczędzasz", "100"}, rows.get(1));
        }

        @Test
        void shouldShowZeroSpentWhenMonthlyRevenueIsNegative() throws IOException {
            when(monthlyRevenueDto.amount()).thenReturn(new BigDecimal("-100.00"));
            when(document.formatPercent(any(BigDecimal.class))).thenAnswer(invocation -> invocation.getArgument(0).toString());

            handler.generate(document, context);

            verify(document, times(2)).addTable(headersCaptor.capture(), rowsCaptor.capture());
            List<String[]> rows = rowsCaptor.getAllValues().get(1);
            assertArrayEquals(new String[]{"Z przychodów wydajesz", "0"}, rows.get(0));
            assertArrayEquals(new String[]{"Z przychodów oszczędzasz", "100"}, rows.get(1));
        }

        @Test
        void shouldShowZeroSavedWhenExpensesExceedRevenues() throws IOException {
            when(monthlyExpenseDto.amount()).thenReturn(new BigDecimal("1500.00"));
            when(document.formatPercent(any(BigDecimal.class))).thenAnswer(invocation -> invocation.getArgument(0).toString());

            handler.generate(document, context);

            verify(document, times(2)).addTable(headersCaptor.capture(), rowsCaptor.capture());
            List<String[]> rows = rowsCaptor.getAllValues().get(1);
            assertArrayEquals(new String[]{"Z przychodów wydajesz", "150.00"}, rows.get(0));
            assertArrayEquals(new String[]{"Z przychodów oszczędzasz", "0"}, rows.get(1));
        }

        @Test
        void shouldShowZeroSpentWhenMonthlyExpenseIsZero() throws IOException {
            when(monthlyExpenseDto.amount()).thenReturn(BigDecimal.ZERO);
            when(document.formatPercent(any(BigDecimal.class))).thenAnswer(invocation -> invocation.getArgument(0).toString());

            handler.generate(document, context);

            verify(document, times(2)).addTable(headersCaptor.capture(), rowsCaptor.capture());
            List<String[]> rows = rowsCaptor.getAllValues().get(1);
            assertArrayEquals(new String[]{"Z przychodów wydajesz", "0.00"}, rows.get(0));
            assertArrayEquals(new String[]{"Z przychodów oszczędzasz", "100.00"}, rows.get(1));
        }

        @Test
        void shouldThrowExceptionWhenExpenseCategoryIsUnknown() {
            when(report.expenseCategoryPercentages()).thenReturn(List.of(foodExpense));
            when(foodExpense.category()).thenReturn("UNKNOWN");
            when(foodExpense.percentage()).thenReturn(FOOD_PERCENTAGE);

            assertThrows(IllegalArgumentException.class, () -> handler.generate(document, context));
        }

        @Test
        void shouldThrowExceptionWhenDocumentFailsOnSection() throws IOException {
            doThrow(IOException.class).when(document).addSection(anyString());

            assertThrows(IOException.class, () -> handler.generate(document, context));
        }

        @Test
        void shouldThrowExceptionWhenDocumentFailsOnTable() throws IOException {
            doThrow(IOException.class).when(document).addTable(any(), any());

            assertThrows(IOException.class, () -> handler.generate(document, context));
        }

        @Nested
        class WithPercentages {

            @BeforeEach
            void setUp() {
                when(report.expenseCategoryPercentages())
                        .thenReturn(List.of(foodExpense, transportExpense, housingExpense, zeroExpense));
                when(report.revenueCategoryPercentages()).thenReturn(List.of(salaryRevenue, businessRevenue));
                when(foodExpense.category()).thenReturn("FOOD");
                when(foodExpense.percentage()).thenReturn(FOOD_PERCENTAGE);
                when(transportExpense.category()).thenReturn("TRANSPORT");
                when(transportExpense.percentage()).thenReturn(TRANSPORT_PERCENTAGE);
                when(housingExpense.category()).thenReturn("HOUSING");
                when(housingExpense.percentage()).thenReturn(HOUSING_PERCENTAGE);
                when(zeroExpense.category()).thenReturn("OTHERS");
                when(zeroExpense.percentage()).thenReturn(ZERO_PERCENTAGE);
                when(salaryRevenue.category()).thenReturn("SALARY");
                when(salaryRevenue.percentage()).thenReturn(SALARY_PERCENTAGE);
                when(businessRevenue.percentage()).thenReturn(BUSINESS_PERCENTAGE);
            }

            @Test
            void shouldAddPieChartWithExpenseStructureWhenPercentagesExist() throws IOException {
                handler.generate(document, context);

                verify(document).addPieChart("Struktura wydatków w tym miesiącu",
                        List.of("Żywność", "Transport", "Mieszkanie", "Inne"),
                        List.of(FOOD_PERCENTAGE, TRANSPORT_PERCENTAGE, HOUSING_PERCENTAGE, ZERO_PERCENTAGE));
            }

            @Test
            void shouldShowMainRevenueSourceWhenRevenuePercentagesExist() throws IOException {
                handler.generate(document, context);

                verify(document, times(2)).addTable(headersCaptor.capture(), rowsCaptor.capture());
                List<String[]> rows = rowsCaptor.getAllValues().get(1);
                assertArrayEquals(new String[]{"Główne źródło przychodu", "Pensja (60.00%)"}, rows.get(2));
            }

            @Test
            void shouldShowHighestExpenseWhenExpensePercentagesExist() throws IOException {
                handler.generate(document, context);

                verify(document, times(2)).addTable(headersCaptor.capture(), rowsCaptor.capture());
                List<String[]> rows = rowsCaptor.getAllValues().get(1);
                assertArrayEquals(new String[]{"Największy procent wydatków stanowi", "Mieszkanie (50.00%)"}, rows.get(3));
                assertArrayEquals(new String[]{"Najwięcej wydajesz na", "Mieszkanie (50.00%)"}, rows.get(4));
            }

            @Test
            void shouldShowLowestPositiveExpenseWhenZeroPercentageExists() throws IOException {
                handler.generate(document, context);

                verify(document, times(2)).addTable(headersCaptor.capture(), rowsCaptor.capture());
                List<String[]> rows = rowsCaptor.getAllValues().get(1);
                assertArrayEquals(new String[]{"Najmniej wydajesz na", "Transport (10.00%)"}, rows.get(5));
            }

            @Test
            void shouldAddAllDetailedRowsWhenPercentagesExist() throws IOException {
                when(document.formatPercent(any(BigDecimal.class))).thenAnswer(invocation -> invocation.getArgument(0).toString());

                handler.generate(document, context);

                verify(document, times(2)).addTable(headersCaptor.capture(), rowsCaptor.capture());
                List<String[]> rows = rowsCaptor.getAllValues().get(1);
                assertEquals(6, rows.size());
                assertArrayEquals(new String[]{"Z przychodów wydajesz", "25.00"}, rows.get(0));
                assertArrayEquals(new String[]{"Z przychodów oszczędzasz", "75.00"}, rows.get(1));
            }
        }
    }
}