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
class SumFinancesPdfTest {

    private static final BigDecimal REVENUE = new BigDecimal("500.00");
    private static final BigDecimal EXPENSE = new BigDecimal("200.00");
    private static final BigDecimal BALANCE = new BigDecimal("300.00");
    private static final List<String> CHART_LABELS = List.of("Przychody", "Wydatki", "Bilans");

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
    private SumFinancesPdf handler;

    @Nested
    class GetType {

        @Test
        void shouldReturnSumFinancesTypeWhenTypeIsRequested() {
            assertEquals(PdfReportType.SUM_FINANCES, handler.getType());
        }
    }

    @Nested
    class GetTitle {

        @ParameterizedTest
        @EnumSource(PeriodType.class)
        void shouldReturnTitleWhenPeriodTypeIsGiven(PeriodType periodType) {
            assertEquals("Zsumowane przychody i wydatki", handler.getTitle(periodType));
        }

        @Test
        void shouldReturnTitleWhenPeriodTypeIsNull() {
            assertEquals("Zsumowane przychody i wydatki", handler.getTitle(null));
        }
    }

    @Nested
    class GetFileName {

        @ParameterizedTest
        @CsvSource({
                "DAILY,zsumowane-przychody-wydatki-daily.pdf",
                "WEEKLY,zsumowane-przychody-wydatki-weekly.pdf",
                "MONTHLY,zsumowane-przychody-wydatki-monthly.pdf"
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
        void shouldAddZeroBalanceWhenTotalsAreNull() throws IOException {
            handler.generate(document, context);

            verify(document).addSummary("Bilans", BigDecimal.ZERO);
        }

        @Test
        void shouldAddZeroValuesToChartWhenTotalsValuesAreNull() throws IOException {
            when(report.totals()).thenReturn(totals);

            handler.generate(document, context);

            verify(document).addLineChart("Porównanie przepływu", CHART_LABELS,
                    List.of(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO), true);
        }

        @Test
        void shouldAddNegativeBalanceWhenExpensesExceedRevenues() throws IOException {
            when(report.totals()).thenReturn(totals);
            when(totals.revenues()).thenReturn(EXPENSE);
            when(totals.expenses()).thenReturn(REVENUE);

            handler.generate(document, context);

            verify(document).addSummary("Bilans", new BigDecimal("-300.00"));
        }

        @Test
        void shouldThrowExceptionWhenPeriodTypeIsNull() {
            when(context.periodType()).thenReturn(null);

            assertThrows(NullPointerException.class, () -> handler.generate(document, context));
        }

        @Test
        void shouldThrowExceptionWhenDocumentFails() throws IOException {
            doThrow(IOException.class).when(document).addInfo(anyString(), anyString());

            assertThrows(IOException.class, () -> handler.generate(document, context));
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

                verify(document).addSection("Podsumowanie");
            }

            @Test
            void shouldAddMonthlyPeriodLabelWhenPeriodIsMonthly() throws IOException {
                handler.generate(document, context);

                verify(document).addInfo("Okres:", "Miesięczny");
            }

            @Test
            void shouldAddLineChartWithTotalsWhenTotalsExist() throws IOException {
                handler.generate(document, context);

                verify(document).addLineChart("Porównanie przepływu", CHART_LABELS,
                        List.of(REVENUE, EXPENSE, BALANCE), true);
            }

            @Test
            void shouldAddTableWithFormattedTotalsWhenTotalsExist() throws IOException {
                when(document.formatMoney(REVENUE)).thenReturn("500,00 zł");
                when(document.formatMoney(EXPENSE)).thenReturn("200,00 zł");

                handler.generate(document, context);

                verify(document).addTable(headersCaptor.capture(), rowsCaptor.capture());
                List<String[]> rows = rowsCaptor.getValue();
                assertArrayEquals(new String[]{"Typ", "Wartość"}, headersCaptor.getValue());
                assertEquals(2, rows.size());
                assertArrayEquals(new String[]{"Przychody", "500,00 zł"}, rows.get(0));
                assertArrayEquals(new String[]{"Wydatki", "200,00 zł"}, rows.get(1));
            }

            @Test
            void shouldAddSummaryWithBalanceWhenTotalsExist() throws IOException {
                handler.generate(document, context);

                verify(document).addSummary("Bilans", BALANCE);
            }

            @Test
            void shouldAddElementsInOrderWhenReportIsGenerated() throws IOException {
                handler.generate(document, context);

                InOrder inOrder = inOrder(document);
                inOrder.verify(document).addSection("Podsumowanie");
                inOrder.verify(document).addInfo("Okres:", "Miesięczny");
                inOrder.verify(document).addLineChart(anyString(), any(), any(), anyBoolean());
                inOrder.verify(document).addTable(any(), any());
                inOrder.verify(document).addSummary("Bilans", BALANCE);
            }
        }
    }
}