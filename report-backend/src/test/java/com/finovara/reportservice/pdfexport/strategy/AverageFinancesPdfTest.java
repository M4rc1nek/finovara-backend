package com.finovara.reportservice.pdfexport.strategy;

import com.finovara.contracts.mainaccount.report.financial.dto.FinancialReportDto;
import com.finovara.contracts.mainaccount.report.financial.dto.FinancialTotalsDto;
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
class AverageFinancesPdfTest {

    private static final BigDecimal REVENUE = new BigDecimal("300.00");
    private static final BigDecimal EXPENSE = new BigDecimal("100.00");
    private static final BigDecimal AVERAGE_REVENUE = PdfReportValues.averagePerDay(REVENUE, PeriodType.DAILY);
    private static final BigDecimal AVERAGE_EXPENSE = PdfReportValues.averagePerDay(EXPENSE, PeriodType.DAILY);
    private static final BigDecimal AVERAGE_DIFFERENCE = AVERAGE_REVENUE.subtract(AVERAGE_EXPENSE);
    private static final BigDecimal ZERO_AVERAGE = new BigDecimal("0.00");
    private static final List<String> CHART_LABELS = List.of("Przychody", "Wydatki", "Różnica średnich");

    @Mock
    private PdfReportDocument document;

    @Mock
    private PdfReportContext context;

    @Mock
    private FinancialReportDto report;

    @Mock
    private FinancialTotalsDto totals;

    @Captor
    private ArgumentCaptor<String[]> headersCaptor;

    @Captor
    private ArgumentCaptor<List<String[]>> rowsCaptor;

    @InjectMocks
    private AverageFinancesPdf handler;

    @Nested
    class GetType {

        @Test
        void shouldReturnAverageFinancesTypeWhenTypeIsRequested() {
            assertEquals(PdfReportType.AVERAGE_FINANCES, handler.getType());
        }
    }

    @Nested
    class GetTitle {

        @ParameterizedTest
        @EnumSource(PeriodType.class)
        void shouldReturnTitleWhenPeriodTypeIsGiven(PeriodType periodType) {
            assertEquals("Średnie przychody i wydatki", handler.getTitle(periodType));
        }

        @Test
        void shouldReturnTitleWhenPeriodTypeIsNull() {
            assertEquals("Średnie przychody i wydatki", handler.getTitle(null));
        }
    }

    @Nested
    class GetFileName {

        @ParameterizedTest
        @CsvSource({
                "DAILY,srednie-przychody-wydatki-daily.pdf",
                "WEEKLY,srednie-przychody-wydatki-weekly.pdf",
                "MONTHLY,srednie-przychody-wydatki-monthly.pdf"
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
            when(context.periodType()).thenReturn(PeriodType.DAILY);
        }

        @Test
        void shouldAddWeeklyPeriodLabelWhenPeriodIsWeekly() throws IOException {
            when(context.periodType()).thenReturn(PeriodType.WEEKLY);

            handler.generate(document, context);

            verify(document).addInfo("Okres:", "Tygodniowy");
        }

        @Test
        void shouldAddMonthlyPeriodLabelWhenPeriodIsMonthly() throws IOException {
            when(context.periodType()).thenReturn(PeriodType.MONTHLY);

            handler.generate(document, context);

            verify(document).addInfo("Okres:", "Miesięczny");
        }

        @Test
        void shouldThrowExceptionWhenPeriodTypeIsNull() {
            when(context.periodType()).thenReturn(null);

            assertThrows(NullPointerException.class, () -> handler.generate(document, context));
        }

        @Test
        void shouldThrowExceptionWhenDocumentFails() throws IOException {
            doThrow(IOException.class).when(document).addSection(anyString());

            assertThrows(IOException.class, () -> handler.generate(document, context));
        }

        @Nested
        class WithMissingTotals {

            @Test
            void shouldAddZeroAveragesWhenTotalsAreNull() throws IOException {
                handler.generate(document, context);

                verify(document).addLineChart("Relacja średnich wartości", CHART_LABELS,
                        List.of(ZERO_AVERAGE, ZERO_AVERAGE, ZERO_AVERAGE), true);
            }

            @Test
            void shouldAddZeroAveragesWhenTotalsValuesAreNull() throws IOException {
                when(report.totals()).thenReturn(totals);

                handler.generate(document, context);

                verify(document).addSummary("Różnica średnich", ZERO_AVERAGE);
            }

            @Test
            void shouldAddNegativeDifferenceWhenExpensesExceedRevenues() throws IOException {
                when(report.totals()).thenReturn(totals);
                when(totals.revenues()).thenReturn(EXPENSE);
                when(totals.expenses()).thenReturn(REVENUE);

                handler.generate(document, context);

                verify(document).addSummary("Różnica średnich", AVERAGE_EXPENSE.subtract(AVERAGE_REVENUE));
            }
        }

        @Nested
        class WithTotals {

            @BeforeEach
            void setUp() {
                when(report.totals()).thenReturn(totals);
                when(totals.revenues()).thenReturn(REVENUE);
                when(totals.expenses()).thenReturn(EXPENSE);
            }

            @Test
            void shouldAddSectionWhenReportIsGenerated() throws IOException {
                handler.generate(document, context);

                verify(document).addSection("Średnie wartości");
            }

            @Test
            void shouldAddDailyPeriodLabelWhenPeriodIsDaily() throws IOException {
                handler.generate(document, context);

                verify(document).addInfo("Okres:", "Dzienny");
            }

            @Test
            void shouldAddLineChartWithAveragesWhenTotalsExist() throws IOException {
                handler.generate(document, context);

                verify(document).addLineChart("Relacja średnich wartości", CHART_LABELS,
                        List.of(AVERAGE_REVENUE, AVERAGE_EXPENSE, AVERAGE_DIFFERENCE), true);
            }

            @Test
            void shouldAddTableWithFormattedAveragesWhenTotalsExist() throws IOException {
                when(document.formatMoney(AVERAGE_REVENUE)).thenReturn("300,00 zł");
                when(document.formatMoney(AVERAGE_EXPENSE)).thenReturn("100,00 zł");

                handler.generate(document, context);

                verify(document).addTable(headersCaptor.capture(), rowsCaptor.capture());
                List<String[]> rows = rowsCaptor.getValue();
                assertArrayEquals(new String[]{"Typ", "Średnia dzienna wartość"}, headersCaptor.getValue());
                assertEquals(2, rows.size());
                assertArrayEquals(new String[]{"Przychody", "300,00 zł"}, rows.get(0));
                assertArrayEquals(new String[]{"Wydatki", "100,00 zł"}, rows.get(1));
            }

            @Test
            void shouldAddSummaryWithDifferenceWhenTotalsExist() throws IOException {
                handler.generate(document, context);

                verify(document).addSummary("Różnica średnich", AVERAGE_DIFFERENCE);
            }

            @Test
            void shouldAddElementsInOrderWhenReportIsGenerated() throws IOException {
                handler.generate(document, context);

                InOrder inOrder = inOrder(document);
                inOrder.verify(document).addSection("Średnie wartości");
                inOrder.verify(document).addInfo("Okres:", "Dzienny");
                inOrder.verify(document).addLineChart(anyString(), any(), any(), anyBoolean());
                inOrder.verify(document).addTable(any(), any());
                inOrder.verify(document).addSummary("Różnica średnich", AVERAGE_DIFFERENCE);
            }
        }
    }
}