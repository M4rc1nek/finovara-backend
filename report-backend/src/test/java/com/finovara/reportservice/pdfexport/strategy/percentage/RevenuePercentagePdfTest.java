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
class RevenuePercentagePdfTest {

    private static final String[] HEADERS = {"Kategoria", "Udział"};
    private static final BigDecimal SALARY_PERCENTAGE = new BigDecimal("80.00");
    private static final BigDecimal GIFT_PERCENTAGE = new BigDecimal("20.00");

    @Mock
    private PdfReportDocument document;

    @Mock
    private PdfReportContext context;

    @Mock
    private FinancialReportDto report;

    @Mock
    private FinancialCategoryPercentageDto salaryRevenue;

    @Mock
    private FinancialCategoryPercentageDto giftRevenue;

    @Captor
    private ArgumentCaptor<String[]> headersCaptor;

    @Captor
    private ArgumentCaptor<List<String[]>> rowsCaptor;

    @InjectMocks
    private RevenuePercentagePdf handler;

    @Nested
    class GetType {

        @Test
        void shouldReturnPercentageOfRevenuesTypeWhenTypeIsRequested() {
            assertEquals(PdfReportType.PERCENTAGE_OF_REVENUES, handler.getType());
        }
    }

    @Nested
    class GetTitle {

        @ParameterizedTest
        @EnumSource(PeriodType.class)
        void shouldReturnTitleWhenPeriodTypeIsGiven(PeriodType periodType) {
            assertEquals("Udział procentowy przychodów", handler.getTitle(periodType));
        }

        @Test
        void shouldReturnTitleWhenPeriodTypeIsNull() {
            assertEquals("Udział procentowy przychodów", handler.getTitle(null));
        }
    }

    @Nested
    class GetFileName {

        @ParameterizedTest
        @CsvSource({
                "DAILY,udzial-procentowy-przychodow-daily.pdf",
                "WEEKLY,udzial-procentowy-przychodow-weekly.pdf",
                "MONTHLY,udzial-procentowy-przychodow-monthly.pdf"
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
        void shouldAddEmptyPieChartWhenPercentagesAreNull() throws IOException {
            when(report.revenueCategoryPercentages()).thenReturn(null);

            handler.generate(document, context);

            verify(document).addPieChart("Struktura przychodów", List.of(), List.of());
        }

        @Test
        void shouldAddEmptyPieChartWhenPercentagesAreEmpty() throws IOException {
            when(report.revenueCategoryPercentages()).thenReturn(List.of());

            handler.generate(document, context);

            verify(document).addPieChart("Struktura przychodów", List.of(), List.of());
        }

        @Test
        void shouldAddEmptyTableWhenPercentagesAreEmpty() throws IOException {
            when(report.revenueCategoryPercentages()).thenReturn(List.of());

            handler.generate(document, context);

            verify(document).addTable(headersCaptor.capture(), rowsCaptor.capture());
            assertArrayEquals(HEADERS, headersCaptor.getValue());
            assertEquals(0, rowsCaptor.getValue().size());
        }

        @Test
        void shouldUseOthersLabelWhenCategoryIsNull() throws IOException {
            when(report.revenueCategoryPercentages()).thenReturn(List.of(salaryRevenue));
            when(salaryRevenue.category()).thenReturn(null);
            when(salaryRevenue.percentage()).thenReturn(SALARY_PERCENTAGE);

            handler.generate(document, context);

            verify(document).addPieChart("Struktura przychodów", List.of("Inne"), List.of(SALARY_PERCENTAGE));
        }

        @Test
        void shouldThrowExceptionWhenCategoryIsUnknown() {
            when(report.revenueCategoryPercentages()).thenReturn(List.of(salaryRevenue));
            when(salaryRevenue.category()).thenReturn("UNKNOWN");

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
                when(report.revenueCategoryPercentages()).thenReturn(List.of(salaryRevenue, giftRevenue));
                when(salaryRevenue.category()).thenReturn("SALARY");
                when(salaryRevenue.percentage()).thenReturn(SALARY_PERCENTAGE);
                when(giftRevenue.category()).thenReturn("GIFT");
                when(giftRevenue.percentage()).thenReturn(GIFT_PERCENTAGE);
            }

            @Test
            void shouldAddSectionWhenReportIsGenerated() throws IOException {
                handler.generate(document, context);

                verify(document).addSection("Udział przychodów według kategorii");
            }

            @Test
            void shouldAddMonthlyPeriodLabelWhenPeriodIsMonthly() throws IOException {
                handler.generate(document, context);

                verify(document).addInfo("Okres:", "Miesięczny");
            }

            @Test
            void shouldAddPieChartWithLabelsAndPercentagesWhenPercentagesExist() throws IOException {
                handler.generate(document, context);

                verify(document).addPieChart("Struktura przychodów",
                        List.of("Pensja", "Prezent"), List.of(SALARY_PERCENTAGE, GIFT_PERCENTAGE));
            }

            @Test
            void shouldAddTableWithFormattedPercentagesWhenPercentagesExist() throws IOException {
                when(document.formatPercent(SALARY_PERCENTAGE)).thenReturn("80,00%");
                when(document.formatPercent(GIFT_PERCENTAGE)).thenReturn("20,00%");

                handler.generate(document, context);

                verify(document).addTable(headersCaptor.capture(), rowsCaptor.capture());
                List<String[]> rows = rowsCaptor.getValue();
                assertArrayEquals(HEADERS, headersCaptor.getValue());
                assertEquals(2, rows.size());
                assertArrayEquals(new String[]{"Pensja", "80,00%"}, rows.get(0));
                assertArrayEquals(new String[]{"Prezent", "20,00%"}, rows.get(1));
            }

            @Test
            void shouldAddElementsInOrderWhenReportIsGenerated() throws IOException {
                handler.generate(document, context);

                InOrder inOrder = inOrder(document);
                inOrder.verify(document).addSection("Udział przychodów według kategorii");
                inOrder.verify(document).addInfo("Okres:", "Miesięczny");
                inOrder.verify(document).addPieChart(anyString(), any(), any());
                inOrder.verify(document).addTable(any(), any());
            }
        }
    }
}