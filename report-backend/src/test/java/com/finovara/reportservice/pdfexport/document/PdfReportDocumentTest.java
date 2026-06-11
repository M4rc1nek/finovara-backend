package com.finovara.reportservice.pdfexport.document;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class PdfReportDocumentTest {

    private PdfReportDocument document;

    @BeforeEach
    void setUp() throws IOException {
        document = new PdfReportDocument("Test Report");
    }

    @AfterEach
    void tearDown() throws IOException {
        document.close();
    }

    @Nested
    class FormatMoney {

        @Test
        void shouldFormatPositiveAmountWithPolishLocale() {
            assertThat(document.formatMoney(new BigDecimal("12345.678"))).isEqualTo("12 345,68 PLN");
        }

        @Test
        void shouldReturnZeroWhenAmountIsNull() {
            assertThat(document.formatMoney(null)).isEqualTo("0,00 PLN");
        }

        @Test
        void shouldRoundUsingHalfUpStrategy() {
            assertThat(document.formatMoney(new BigDecimal("12.345"))).isEqualTo("12,35 PLN");
        }

        @Test
        void shouldFormatNegativeAmount() {
            assertThat(document.formatMoney(new BigDecimal("-1234.5"))).isEqualTo("-1 234,50 PLN");
        }
    }

    @Nested
    class FormatPercent {

        @Test
        void shouldFormatPositivePercentageWithPolishLocale() {
            assertThat(document.formatPercent(new BigDecimal("15.236"))).isEqualTo("15,24%");
        }

        @Test
        void shouldReturnZeroWhenPercentageIsNull() {
            assertThat(document.formatPercent(null)).isEqualTo("0,00%");
        }

        @Test
        void shouldRoundUsingHalfUpStrategy() {
            assertThat(document.formatPercent(new BigDecimal("99.994"))).isEqualTo("99,99%");
        }

        @Test
        void shouldFormatNegativePercentage() {
            assertThat(document.formatPercent(new BigDecimal("-12.1"))).isEqualTo("-12,10%");
        }
    }

    @Nested
    class ToByteArray {

        @Test
        void shouldGenerateValidPdfDocument() throws IOException {
            document.addSection("Summary");

            byte[] pdfBytes = document.toByteArray();

            assertThat(pdfBytes).isNotEmpty();
            try (PDDocument pdf = Loader.loadPDF(pdfBytes)) {
                assertThat(pdf.getNumberOfPages()).isGreaterThan(0);
            }
        }

        @Test
        void shouldGeneratePdfImmediatelyAfterCreation() throws IOException {
            byte[] pdfBytes = document.toByteArray();

            assertThat(pdfBytes).isNotEmpty();
            try (PDDocument pdf = Loader.loadPDF(pdfBytes)) {
                assertThat(pdf.getNumberOfPages()).isEqualTo(1);
            }
        }

        @Test
        void shouldCreateMultiplePagesWhenContentExceedsPageSize() throws IOException {
            for (int i = 0; i < 100; i++) {
                document.addInfo("Label " + i, "Value " + i);
            }

            byte[] pdfBytes = document.toByteArray();

            try (PDDocument pdf = Loader.loadPDF(pdfBytes)) {
                assertThat(pdf.getNumberOfPages()).isGreaterThan(1);
            }
        }

        @Test
        void shouldAllowCallingMultipleTimes() throws IOException {
            byte[] first = document.toByteArray();
            byte[] second = document.toByteArray();

            assertThat(first).isNotEmpty();
            assertThat(second).isNotEmpty();
        }
    }

    @Nested
    class AddTable {

        @Test
        void shouldAddTableWithoutThrowingException() {
            String[] headers = {"Name", "Amount"};
            List<String[]> rows = List.of(new String[]{"Food", "100"}, new String[]{"Bills", "200"});

            assertThatCode(() -> document.addTable(headers, rows)).doesNotThrowAnyException();
        }

        @Test
        void shouldAddEmptyTableWithoutThrowingException() {
            assertThatCode(() -> document.addTable(new String[]{"Name", "Amount"}, List.of()))
                    .doesNotThrowAnyException();
        }

        @Test
        void shouldHandleNullCellValues() {
            List<String[]> rows = List.<String[]>of(new String[]{null, null});

            assertThatCode(() -> document.addTable(new String[]{"Name", "Amount"}, rows))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    class AddBarChart {

        @Test
        void shouldAddBarChartWithoutThrowingException() {
            List<String> labels = List.of("Jan", "Feb", "Mar");
            List<BigDecimal> values = List.of(new BigDecimal("100"), new BigDecimal("200"), new BigDecimal("150"));

            assertThatCode(() -> document.addBarChart("Income", labels, values, true))
                    .doesNotThrowAnyException();
        }

        @Test
        void shouldHandleEmptyValues() {
            assertThatCode(() -> document.addBarChart("Income", List.of(), List.of(), true))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    class AddLineChart {

        @Test
        void shouldAddLineChartWithoutThrowingException() {
            List<String> labels = List.of("Jan", "Feb", "Mar");
            List<BigDecimal> values = List.of(new BigDecimal("10"), new BigDecimal("20"), new BigDecimal("15"));

            assertThatCode(() -> document.addLineChart("Trend", labels, values, false))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    class AddPieChart {

        @Test
        void shouldAddPieChartWithoutThrowingException() {
            List<String> labels = List.of("Food", "Bills", "Savings");
            List<BigDecimal> values = List.of(new BigDecimal("40"), new BigDecimal("35"), new BigDecimal("25"));

            assertThatCode(() -> document.addPieChart("Expenses", labels, values))
                    .doesNotThrowAnyException();
        }

        @Test
        void shouldHandleNullValuesInSliceData() {
            List<String> labels = List.of("Jan", "Feb");
            List<BigDecimal> values = new ArrayList<>();
            values.add(null);
            values.add(new BigDecimal("100"));

            assertThatCode(() -> document.addPieChart("Pie", labels, values))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    class AddInfo {

        @Test
        void shouldAddInfoWithoutThrowingException() {
            assertThatCode(() -> document.addInfo("Name", "John Doe")).doesNotThrowAnyException();
        }
    }

    @Nested
    class AddSummary {

        @Test
        void shouldAddSummaryWithoutThrowingException() {
            assertThatCode(() -> document.addSummary("Balance", new BigDecimal("9999.99")))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    class Close {

        @Test
        void shouldCloseWithoutException() {
            assertThatCode(() -> document.close()).doesNotThrowAnyException();
        }
    }
}
