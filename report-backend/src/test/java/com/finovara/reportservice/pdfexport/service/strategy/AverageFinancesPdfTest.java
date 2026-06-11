package com.finovara.reportservice.pdfexport.service.strategy;

import com.finovara.contracts.model.PeriodType;
import com.finovara.reportservice.pdfexport.document.PdfReportDocument;
import com.finovara.reportservice.pdfexport.model.PdfReportType;
import com.finovara.reportservice.pdfexport.service.strategy.label.PdfReportText;
import com.finovara.reportservice.report.dto.ReportDto;
import com.finovara.reportservice.report.finances.average.service.ReportAverageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AverageFinancesPdfTest {

    private static final Long USER_ID = 1L;
    private static final PeriodType PERIOD_TYPE = PeriodType.MONTHLY;
    private static final BigDecimal REVENUE_AMOUNT = new BigDecimal("1000");
    private static final BigDecimal EXPENSE_AMOUNT = new BigDecimal("400");
    private static final BigDecimal DIFFERENCE = new BigDecimal("600");

    @Mock
    private ReportAverageService reportAverageService;

    @Mock
    private PdfReportDocument document;

    private AverageFinancesPdf averageFinancesPdf;

    @BeforeEach
    void setUp() {
        averageFinancesPdf = new AverageFinancesPdf(reportAverageService);
    }

    @Nested
    class GetType {

        @Test
        void shouldReturnAverageFinancesType() {
            assertThat(averageFinancesPdf.getType()).isEqualTo(PdfReportType.AVERAGE_FINANCES);
        }
    }

    @Nested
    class GetTitle {

        @Test
        void shouldReturnConstantTitle() {
            assertThat(averageFinancesPdf.getTitle(PERIOD_TYPE)).isEqualTo("Średnie przychody i wydatki");
        }
    }

    @Nested
    class GetFileName {

        @Test
        void shouldContainBaseName() {
            assertThat(averageFinancesPdf.getFileName(PERIOD_TYPE)).contains("srednie-przychody-wydatki");
        }
    }

    @Nested
    class Generate {

        @ParameterizedTest
        @EnumSource(PeriodType.class)
        void shouldBuildReportFromAverageService(PeriodType periodType) throws Exception {
            ReportDto revenue = new ReportDto(periodType, REVENUE_AMOUNT);
            ReportDto expense = new ReportDto(periodType, EXPENSE_AMOUNT);

            when(reportAverageService.calculateAverageRevenue(USER_ID, periodType)).thenReturn(revenue);
            when(reportAverageService.calculateAverageExpense(USER_ID, periodType)).thenReturn(expense);
            when(document.formatMoney(REVENUE_AMOUNT)).thenReturn("1000");
            when(document.formatMoney(EXPENSE_AMOUNT)).thenReturn("400");

            averageFinancesPdf.generate(document, USER_ID, periodType);

            verify(document).addSection("Średnie wartości");
            verify(document).addInfo("Okres:", PdfReportText.periodLabel(periodType));
            verify(document).addLineChart(
                    eq("Relacja średnich wartości"),
                    eq(List.of("Przychody", "Wydatki", "Różnica średnich")),
                    eq(List.of(REVENUE_AMOUNT, EXPENSE_AMOUNT, DIFFERENCE)),
                    eq(true)
            );
            verify(document).addTable(eq(new String[]{"Typ", "Średnia wartość"}), anyList());
            verify(document).addSummary("Różnica średnich", DIFFERENCE);
        }

        @Test
        void shouldHandleZeroAmounts() throws Exception {
            ReportDto revenue = new ReportDto(PERIOD_TYPE, BigDecimal.ZERO);
            ReportDto expense = new ReportDto(PERIOD_TYPE, BigDecimal.ZERO);

            when(reportAverageService.calculateAverageRevenue(USER_ID, PERIOD_TYPE)).thenReturn(revenue);
            when(reportAverageService.calculateAverageExpense(USER_ID, PERIOD_TYPE)).thenReturn(expense);
            when(document.formatMoney(BigDecimal.ZERO)).thenReturn("0");

            averageFinancesPdf.generate(document, USER_ID, PERIOD_TYPE);

            verify(document).addSummary("Różnica średnich", BigDecimal.ZERO);
            verify(document).addLineChart(
                    eq("Relacja średnich wartości"),
                    eq(List.of("Przychody", "Wydatki", "Różnica średnich")),
                    eq(List.of(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO)),
                    eq(true)
            );
        }

        @Test
        void shouldHandleNegativeDifference() throws Exception {
            ReportDto revenue = new ReportDto(PERIOD_TYPE, new BigDecimal("100"));
            ReportDto expense = new ReportDto(PERIOD_TYPE, new BigDecimal("300"));

            when(reportAverageService.calculateAverageRevenue(USER_ID, PERIOD_TYPE)).thenReturn(revenue);
            when(reportAverageService.calculateAverageExpense(USER_ID, PERIOD_TYPE)).thenReturn(expense);
            when(document.formatMoney(new BigDecimal("100"))).thenReturn("100");
            when(document.formatMoney(new BigDecimal("300"))).thenReturn("300");

            averageFinancesPdf.generate(document, USER_ID, PERIOD_TYPE);

            verify(document).addSummary("Różnica średnich", new BigDecimal("-200"));
            verify(document).addLineChart(
                    eq("Relacja średnich wartości"),
                    eq(List.of("Przychody", "Wydatki", "Różnica średnich")),
                    eq(List.of(new BigDecimal("100"), new BigDecimal("300"), new BigDecimal("-200"))),
                    eq(true)
            );
        }
    }
}
