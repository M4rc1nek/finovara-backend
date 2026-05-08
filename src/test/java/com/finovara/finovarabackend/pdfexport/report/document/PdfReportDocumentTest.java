package com.finovara.finovarabackend.pdfexport.report.document;

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
    class FormattingTests {

        @Test
        void shouldFormatMoneyCorrectly() {
            String result = document.formatMoney(new BigDecimal("12345.678"));

            assertThat(result).isEqualTo("12 345,68 PLN");
        }

        @Test
        void shouldFormatPercentCorrectly() {
            String result = document.formatPercent(new BigDecimal("15.236"));

            assertThat(result).isEqualTo("15,24%");
        }

        @Test
        void shouldReturnZeroMoneyWhenAmountIsNull() {
            String result = document.formatMoney(null);

            assertThat(result).isEqualTo("0,00 PLN");
        }

        @Test
        void shouldReturnZeroPercentWhenPercentageIsNull() {
            String result = document.formatPercent(null);

            assertThat(result).isEqualTo("0,00%");
        }

        @Test
        void shouldRoundMoneyUsingHalfUpStrategy() {
            String result = document.formatMoney(new BigDecimal("12.345"));

            assertThat(result).isEqualTo("12,35 PLN");
        }

        @Test
        void shouldRoundPercentUsingHalfUpStrategy() {
            String result = document.formatPercent(new BigDecimal("99.994"));

            assertThat(result).isEqualTo("99,99%");
        }

        @Test
        void shouldFormatNegativeMoneyCorrectly() {
            String result = document.formatMoney(new BigDecimal("-1234.5"));

            assertThat(result).isEqualTo("-1 234,50 PLN");
        }

        @Test
        void shouldFormatNegativePercentCorrectly() {
            String result = document.formatPercent(new BigDecimal("-12.1"));

            assertThat(result).isEqualTo("-12,10%");
        }
    }

    @Nested
    class PdfGenerationTests {

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
        void shouldGeneratePdfWithTable() throws IOException {
            document.addTable(new String[]{"Category", "Amount"}, List.of(new String[]{"Food", "100"}, new String[]{"Bills", "200"}));

            byte[] bytes = document.toByteArray();

            assertThat(bytes).isNotEmpty();

            try (PDDocument pdf = Loader.loadPDF(bytes)) {
                assertThat(pdf.getNumberOfPages()).isEqualTo(1);
            }
        }

        @Test
        void shouldGeneratePdfWithCharts() throws IOException {
            List<String> labels = List.of("Jan", "Feb", "Mar");

            List<BigDecimal> values = List.of(new BigDecimal("100"), new BigDecimal("200"), new BigDecimal("150"));

            document.addBarChart("Bar", labels, values, true);
            document.addLineChart("Line", labels, values, true);
            document.addPieChart("Pie", labels, values);

            byte[] bytes = document.toByteArray();

            assertThat(bytes).isNotEmpty();

            try (PDDocument pdf = Loader.loadPDF(bytes)) {
                assertThat(pdf.getNumberOfPages()).isGreaterThanOrEqualTo(1);
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
        void shouldGeneratePdfLargerThanMinimalSize() throws IOException {
            document.addSection("Test");
            document.addInfo("User", "John");

            byte[] bytes = document.toByteArray();

            assertThat(bytes.length).isGreaterThan(1000);
        }

        @Test
        void shouldAllowCallingToByteArrayMultipleTimes() throws IOException {
            byte[] first = document.toByteArray();
            byte[] second = document.toByteArray();

            assertThat(first).isNotEmpty();
            assertThat(second).isNotEmpty();
        }
    }

    @Nested
    class TableTests {

        @Test
        void shouldAddTableWithoutThrowingException() {
            String[] headers = {"Name", "Amount"};

            List<String[]> rows = List.of(new String[]{"Food", "100"}, new String[]{"Transport", "50"});

            assertThatCode(() -> document.addTable(headers, rows)).doesNotThrowAnyException();
        }

        @Test
        void shouldAddEmptyTableWithoutThrowingException() {
            String[] headers = {"Name", "Amount"};

            assertThatCode(() -> document.addTable(headers, List.of())).doesNotThrowAnyException();
        }

        @Test
        void shouldHandleVeryLongTextInTable() {
            String longText = "Lorem ipsum dolor sit amet consectetur adipiscing elit sed do eiusmod tempor incididunt";

            List<String[]> rows = new ArrayList<>();
            rows.add(new String[]{longText});

            assertThatCode(() -> document.addTable(new String[]{"Description"}, rows)).doesNotThrowAnyException();
        }

        @Test
        void shouldHandleNullCellValuesInTable() {
            List<String[]> rows = new ArrayList<>();
            rows.add(new String[]{null, null});

            assertThatCode(() -> document.addTable(new String[]{"Name", "Amount"}, rows)).doesNotThrowAnyException();
        }

        @Test
        void shouldHandleLargeDatasetInTable() {
            List<String[]> rows = new ArrayList<>();

            for (int i = 0; i < 500; i++) {
                rows.add(new String[]{"Category " + i, String.valueOf(i)});
            }

            assertThatCode(() -> document.addTable(new String[]{"Name", "Value"}, rows)).doesNotThrowAnyException();
        }
    }

    @Nested
    class ChartTests {

        @Test
        void shouldAddBarChartWithoutThrowingException() {
            List<String> labels = List.of("Jan", "Feb", "Mar");

            List<BigDecimal> values = List.of(new BigDecimal("100"), new BigDecimal("200"), new BigDecimal("150"));

            assertThatCode(() -> document.addBarChart("Income", labels, values, true)).doesNotThrowAnyException();
        }

        @Test
        void shouldAddBarChartWithEmptyValuesWithoutThrowingException() {
            assertThatCode(() -> document.addBarChart("Income", List.of(), List.of(), true)).doesNotThrowAnyException();
        }

        @Test
        void shouldAddLineChartWithoutThrowingException() {
            List<String> labels = List.of("Jan", "Feb", "Mar");

            List<BigDecimal> values = List.of(new BigDecimal("10"), new BigDecimal("20"), new BigDecimal("15"));

            assertThatCode(() -> document.addLineChart("Trend", labels, values, false)).doesNotThrowAnyException();
        }

        @Test
        void shouldAddPieChartWithoutThrowingException() {
            List<String> labels = List.of("Food", "Bills", "Savings");

            List<BigDecimal> values = List.of(new BigDecimal("40"), new BigDecimal("35"), new BigDecimal("25"));

            assertThatCode(() -> document.addPieChart("Expenses", labels, values)).doesNotThrowAnyException();
        }

        @Test
        void shouldAddPieChartWithEmptyValuesWithoutThrowingException() {
            assertThatCode(() -> document.addPieChart("Expenses", List.of(), List.of())).doesNotThrowAnyException();
        }

        @Test
        void shouldHandleZeroValuesInBarChart() {
            List<String> labels = List.of("Jan", "Feb");

            List<BigDecimal> values = List.of(BigDecimal.ZERO, BigDecimal.ZERO);

            assertThatCode(() -> document.addBarChart("Zero chart", labels, values, true)).doesNotThrowAnyException();
        }

        @Test
        void shouldHandleZeroValuesInLineChart() {
            List<String> labels = List.of("Jan", "Feb");

            List<BigDecimal> values = List.of(BigDecimal.ZERO, BigDecimal.ZERO);

            assertThatCode(() -> document.addLineChart("Zero chart", labels, values, true)).doesNotThrowAnyException();
        }

        @Test
        void shouldHandleZeroValuesInPieChart() {
            List<String> labels = List.of("A", "B");

            List<BigDecimal> values = List.of(BigDecimal.ZERO, BigDecimal.ZERO);

            assertThatCode(() -> document.addPieChart("Zero pie", labels, values)).doesNotThrowAnyException();
        }

        @Test
        void shouldHandleNullValuesInCharts() {
            List<String> labels = List.of("Jan", "Feb");

            List<BigDecimal> values = new ArrayList<>();
            values.add(null);
            values.add(new BigDecimal("100"));

            assertThatCode(() -> document.addPieChart("Pie", labels, values)).doesNotThrowAnyException();

            assertThatCode(() -> document.addLineChart("Line", labels, values, true)).doesNotThrowAnyException();

            assertThatCode(() -> document.addBarChart("Bar", labels, values, true)).doesNotThrowAnyException();
        }

        @Test
        void shouldHandleSingleValueLineChart() {
            assertThatCode(() -> document.addLineChart("Single", List.of("Jan"), List.of(new BigDecimal("100")), true)).doesNotThrowAnyException();
        }

        @Test
        void shouldHandleLargeDatasetInBarChart() {
            List<String> labels = new ArrayList<>();
            List<BigDecimal> values = new ArrayList<>();

            for (int i = 0; i < 20; i++) {
                labels.add("L" + i);
                values.add(BigDecimal.valueOf(i + 1));
            }

            assertThatCode(() -> document.addBarChart("Large", labels, values, true)).doesNotThrowAnyException();
        }
    }

    @Nested
    class InfoTests {

        @Test
        void shouldAddInfoWithoutThrowingException() {
            assertThatCode(() -> document.addInfo("Name", "John Doe")).doesNotThrowAnyException();
        }

        @Test
        void shouldAddSummaryWithoutThrowingException() {
            assertThatCode(() -> document.addSummary("Balance", new BigDecimal("9999.99"))).doesNotThrowAnyException();
        }
    }

    @Nested
    class CloseTests {

        @Test
        void shouldCloseWithoutException() {
            assertThatCode(() -> document.close()).doesNotThrowAnyException();
        }
    }
}