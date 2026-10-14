package com.finovara.reportservice.pdfexport.strategy.percentage;

import com.finovara.contracts.mainaccount.report.financial.dto.FinancialCategoryPercentageDto;
import com.finovara.contracts.mainaccount.report.financial.dto.FinancialReportDto;
import com.finovara.contracts.util.PeriodType;
import com.finovara.reportservice.pdfexport.PdfReportContext;
import com.finovara.reportservice.pdfexport.document.PdfReportDocument;
import com.finovara.reportservice.pdfexport.model.PdfReportType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExpensePercentagePdfTest {

    private static final String[] HEADERS = {"Kategoria", "Udział"};
    private static final BigDecimal FOOD_PERCENTAGE = new BigDecimal("65.50");
    private static final BigDecimal BILLS_PERCENTAGE = new BigDecimal("34.50");

    @Mock
    private PdfReportDocument document;

    @Mock
    private PdfReportContext context;

    @Mock
    private FinancialReportDto report;

    @Mock
    private FinancialCategoryPercentageDto foodExpense;

    @Mock
    private FinancialCategoryPercentageDto billsExpense;

    @Captor
    private ArgumentCaptor<String[]> headersCaptor;

    @Captor
    private ArgumentCaptor<List<String[]>> rowsCaptor;

    @InjectMocks
    private ExpensePercentagePdf handler;

    @Nested
    class GetType {

        @Test
        void shouldReturnPercentageOfExpensesTypeWhenTypeIsRequested() {
            assertEquals(PdfReportType.PERCENTAGE_OF_EXPENSES, handler.getType());
        }
    }

    @Nested
    class GetTitle {

        @ParameterizedTest
        @EnumSource(PeriodType.class)
        void shouldReturnTitleWhenPeriodTypeIsGiven(PeriodType periodType) {
            assertEquals("Udział procentowy wydatków", handler.getTitle(periodType));
        }

        @Test
        void shouldReturnTitleWhenPeriodTypeIsNull() {
            assertEquals("Udział procentowy wydatków", handler.getTitle(null));
        }
    }

    @Nested
    class GetFileName {

        @ParameterizedTest
        @CsvSource({
                "DAILY,udzial-procentowy-wydatkow-daily.pdf",
                "WEEKLY,udzial-procentowy-wydatkow-weekly.pdf",
                "MONTHLY,udzial-procentowy-wydatkow-monthly.pdf"
        })
        void shouldReturnFileNameWhenPeriodTypeIsGiven(PeriodType periodType, String expected) {
            assertEquals(expected, handler.getFileName(periodType));
        }

        @Test
        void shouldThrowExceptionWhenPeriodTypeIsNull() {
            assertThrows(NullPointerException.class, () -> handler.getFileName(null));
        }
    }

    @Nested
    class Generate {

        @BeforeEach
        void setUp() {
            when(context.report()).thenReturn(report);
            when(context.periodType()).thenReturn(PeriodType.MONTHLY);
        }

        @Test
        void shouldAddWeeklyPeriodLabelWhenPeriodIsWeekly() throws IOException {
            when(context.periodType()).thenReturn(PeriodType.WEEKLY);

            handler.generate(document, context);

            verify(document).addInfo("Okres:", "Tygodniowy");
        }

        @Test
        void shouldAddEmptyPieChartWhenPercentagesAreNull() throws IOException {
            when(report.expenseCategoryPercentages()).thenReturn(null);

            handler.generate(document, context);

            verify(document).addPieChart("Struktura wydatków", List.of(), List.of());
        }

        @Test
        void shouldAddEmptyPieChartWhenPercentagesAreEmpty() throws IOException {
            when(report.expenseCategoryPercentages()).thenReturn(List.of());

            handler.generate(document, context);

            verify(document).addPieChart("Struktura wydatków", List.of(), List.of());
        }

        @Test
        void shouldAddEmptyTableWhenPercentagesAreEmpty() throws IOException {
            when(report.expenseCategoryPercentages()).thenReturn(List.of());

            handler.generate(document, context);

            verify(document).addTable(headersCaptor.capture(), rowsCaptor.capture());
            assertArrayEquals(HEADERS, headersCaptor.getValue());
            assertEquals(0, rowsCaptor.getValue().size());
        }

        @Test
        void shouldUseOthersLabelWhenCategoryIsNull() throws IOException {
            when(report.expenseCategoryPercentages()).thenReturn(List.of(foodExpense));
            when(foodExpense.category()).thenReturn(null);
            when(foodExpense.percentage()).thenReturn(FOOD_PERCENTAGE);

            handler.generate(document, context);

            verify(document).addPieChart("Struktura wydatków", List.of("Inne"), List.of(FOOD_PERCENTAGE));
        }

        @Test
        void shouldThrowExceptionWhenCategoryIsUnknown() {
            when(report.expenseCategoryPercentages()).thenReturn(List.of(foodExpense));
            when(foodExpense.category()).thenReturn("UNKNOWN");

            assertThrows(IllegalArgumentException.class, () -> handler.generate(document, context));
        }

        @Test
        void shouldThrowExceptionWhenDocumentFails() throws IOException {
            doThrow(IOException.class).when(document).addInfo(anyString(), anyString());

            assertThrows(IOException.class, () -> handler.generate(document, context));
        }

        @Nested
        class WithPercentages {

            @BeforeEach
            void setUp() {
                when(report.expenseCategoryPercentages()).thenReturn(List.of(foodExpense, billsExpense));
                when(foodExpense.category()).thenReturn("FOOD");
                when(foodExpense.percentage()).thenReturn(FOOD_PERCENTAGE);
                when(billsExpense.category()).thenReturn("BILLS");
                when(billsExpense.percentage()).thenReturn(BILLS_PERCENTAGE);
            }

            @Test
            void shouldAddSectionWhenReportIsGenerated() throws IOException {
                handler.generate(document, context);

                verify(document).addSection("Udział wydatków według kategorii");
            }

            @Test
            void shouldAddMonthlyPeriodLabelWhenPeriodIsMonthly() throws IOException {
                handler.generate(document, context);

                verify(document).addInfo("Okres:", "Miesięczny");
            }

            @Test
            void shouldAddPieChartWithLabelsAndPercentagesWhenPercentagesExist() throws IOException {
                handler.generate(document, context);

                verify(document).addPieChart("Struktura wydatków",
                        List.of("Żywność", "Opłaty"), List.of(FOOD_PERCENTAGE, BILLS_PERCENTAGE));
            }

            @Test
            void shouldAddTableWithFormattedPercentagesWhenPercentagesExist() throws IOException {
                when(document.formatPercent(FOOD_PERCENTAGE)).thenReturn("65,50%");
                when(document.formatPercent(BILLS_PERCENTAGE)).thenReturn("34,50%");

                handler.generate(document, context);

                verify(document).addTable(headersCaptor.capture(), rowsCaptor.capture());
                List<String[]> rows = rowsCaptor.getValue();
                assertArrayEquals(HEADERS, headersCaptor.getValue());
                assertEquals(2, rows.size());
                assertArrayEquals(new String[]{"Żywność", "65,50%"}, rows.get(0));
                assertArrayEquals(new String[]{"Opłaty", "34,50%"}, rows.get(1));
            }

            @Test
            void shouldAddElementsInOrderWhenReportIsGenerated() throws IOException {
                handler.generate(document, context);

                InOrder inOrder = inOrder(document);
                inOrder.verify(document).addSection("Udział wydatków według kategorii");
                inOrder.verify(document).addInfo("Okres:", "Miesięczny");
                inOrder.verify(document).addPieChart(anyString(), any(), any());
                inOrder.verify(document).addTable(any(), any());
            }
        }
    }
}