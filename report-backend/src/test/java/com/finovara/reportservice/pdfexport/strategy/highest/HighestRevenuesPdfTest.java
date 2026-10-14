package com.finovara.reportservice.pdfexport.strategy.highest;

import com.finovara.contracts.mainaccount.report.financial.dto.FinancialReportDto;
import com.finovara.contracts.mainaccount.report.financial.dto.HighestRevenueDto;
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
class HighestRevenuesPdfTest {

    private static final String[] HEADERS = {"Kategoria", "Kwota"};
    private static final BigDecimal SALARY_AMOUNT = new BigDecimal("5000.00");
    private static final BigDecimal BONUS_AMOUNT = new BigDecimal("750.25");

    @Mock
    private PdfReportDocument document;

    @Mock
    private PdfReportContext context;

    @Mock
    private FinancialReportDto report;

    @Mock
    private HighestRevenueDto salaryRevenue;

    @Mock
    private HighestRevenueDto bonusRevenue;

    @Captor
    private ArgumentCaptor<String[]> headersCaptor;

    @Captor
    private ArgumentCaptor<List<String[]>> rowsCaptor;

    @InjectMocks
    private HighestRevenuesPdf handler;

    @Nested
    class GetType {

        @Test
        void shouldReturnHighestRevenuesTypeWhenTypeIsRequested() {
            assertEquals(PdfReportType.HIGHEST_REVENUES, handler.getType());
        }
    }

    @Nested
    class GetTitle {

        @ParameterizedTest
        @EnumSource(PeriodType.class)
        void shouldReturnTitleWhenPeriodTypeIsGiven(PeriodType periodType) {
            assertEquals("Największe przychody", handler.getTitle(periodType));
        }

        @Test
        void shouldReturnTitleWhenPeriodTypeIsNull() {
            assertEquals("Największe przychody", handler.getTitle(null));
        }
    }

    @Nested
    class GetFileName {

        @ParameterizedTest
        @CsvSource({
                "DAILY,najwieksze-przychody-daily.pdf",
                "WEEKLY,najwieksze-przychody-weekly.pdf",
                "MONTHLY,najwieksze-przychody-monthly.pdf"
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
        void shouldAddDailyPeriodLabelWhenPeriodIsDaily() throws IOException {
            when(context.periodType()).thenReturn(PeriodType.DAILY);

            handler.generate(document, context);

            verify(document).addInfo("Okres:", "Dzienny");
        }

        @Test
        void shouldAddEmptyChartWhenRevenuesAreNull() throws IOException {
            when(report.highestRevenues()).thenReturn(null);

            handler.generate(document, context);

            verify(document).addBarChart("Największe przychody według kategorii", List.of(), List.of(), true);
        }

        @Test
        void shouldAddEmptyChartWhenRevenuesAreEmpty() throws IOException {
            when(report.highestRevenues()).thenReturn(List.of());

            handler.generate(document, context);

            verify(document).addBarChart("Największe przychody według kategorii", List.of(), List.of(), true);
        }

        @Test
        void shouldAddEmptyTableWhenRevenuesAreEmpty() throws IOException {
            when(report.highestRevenues()).thenReturn(List.of());

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
        class WithRevenues {


            @Test
            void shouldAddSectionWhenReportIsGenerated() throws IOException {
                handler.generate(document, context);

                verify(document).addSection("Największe przychody");
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
                inOrder.verify(document).addSection("Największe przychody");
                inOrder.verify(document).addInfo("Okres:", "Miesięczny");
                inOrder.verify(document).addBarChart(anyString(), any(), any(), anyBoolean());
                inOrder.verify(document).addTable(any(), any());
            }
        }
    }
}