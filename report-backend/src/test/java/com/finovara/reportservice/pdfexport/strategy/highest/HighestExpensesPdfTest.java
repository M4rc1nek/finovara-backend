package com.finovara.reportservice.pdfexport.strategy.highest;

import com.finovara.contracts.mainaccount.report.financial.dto.FinancialReportDto;
import com.finovara.contracts.mainaccount.report.financial.dto.HighestExpenseDto;
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
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HighestExpensesPdfTest {

    private static final String[] HEADERS = {"Kategoria", "Kwota"};
    private static final BigDecimal FOOD_AMOUNT = new BigDecimal("450.00");
    private static final BigDecimal TRANSPORT_AMOUNT = new BigDecimal("120.50");

    @Mock
    private PdfReportDocument document;

    @Mock
    private PdfReportContext context;

    @Mock
    private FinancialReportDto report;

    @Mock
    private HighestExpenseDto foodExpense;

    @Mock
    private HighestExpenseDto transportExpense;

    @Captor
    private ArgumentCaptor<String[]> headersCaptor;

    @Captor
    private ArgumentCaptor<List<String[]>> rowsCaptor;

    @InjectMocks
    private HighestExpensesPdf handler;

    @Nested
    class GetType {

        @Test
        void shouldReturnHighestExpensesTypeWhenTypeIsRequested() {
            assertEquals(PdfReportType.HIGHEST_EXPENSES, handler.getType());
        }
    }

    @Nested
    class GetTitle {

        @ParameterizedTest
        @EnumSource(PeriodType.class)
        void shouldReturnTitleWhenPeriodTypeIsGiven(PeriodType periodType) {
            assertEquals("Największe wydatki", handler.getTitle(periodType));
        }

        @Test
        void shouldReturnTitleWhenPeriodTypeIsNull() {
            assertEquals("Największe wydatki", handler.getTitle(null));
        }
    }

    @Nested
    class GetFileName {

        @ParameterizedTest
        @CsvSource({
                "DAILY,najwieksze-wydatki-daily.pdf",
                "WEEKLY,najwieksze-wydatki-weekly.pdf",
                "MONTHLY,najwieksze-wydatki-monthly.pdf"
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
        void shouldAddEmptyChartWhenExpensesAreNull() throws IOException {
            when(report.highestExpenses()).thenReturn(null);

            handler.generate(document, context);

            verify(document).addBarChart("Największe wydatki według kategorii", List.of(), List.of(), true);
        }

        @Test
        void shouldAddEmptyChartWhenExpensesAreEmpty() throws IOException {
            when(report.highestExpenses()).thenReturn(List.of());

            handler.generate(document, context);

            verify(document).addBarChart("Największe wydatki według kategorii", List.of(), List.of(), true);
        }

        @Test
        void shouldAddEmptyTableWhenExpensesAreEmpty() throws IOException {
            when(report.highestExpenses()).thenReturn(List.of());

            handler.generate(document, context);

            verify(document).addTable(headersCaptor.capture(), rowsCaptor.capture());
            assertArrayEquals(HEADERS, headersCaptor.getValue());
            assertEquals(0, rowsCaptor.getValue().size());
        }

        @Test
        void shouldThrowExceptionWhenDocumentFails() throws IOException {
            doThrow(IOException.class).when(document).addInfo(anyString(), anyString());

            assertThrows(IOException.class, () -> handler.generate(document, context));
        }

        @Nested
        class WithExpenses {

            @Test
            void shouldAddSectionWhenReportIsGenerated() throws IOException {
                handler.generate(document, context);

                verify(document).addSection("Największe wydatki");
            }

            @Test
            void shouldAddMonthlyPeriodLabelWhenPeriodIsMonthly() throws IOException {
                handler.generate(document, context);

                verify(document).addInfo("Okres:", "Miesięczny");
            }


            @Test
            void shouldAddElementsInOrderWhenReportIsGenerated() throws IOException {
                handler.generate(document, context);

                InOrder inOrder = inOrder(document);
                inOrder.verify(document).addSection("Największe wydatki");
                inOrder.verify(document).addInfo("Okres:", "Miesięczny");
                inOrder.verify(document).addBarChart(anyString(), any(), any(), anyBoolean());
                inOrder.verify(document).addTable(any(), any());
            }
        }
    }
}