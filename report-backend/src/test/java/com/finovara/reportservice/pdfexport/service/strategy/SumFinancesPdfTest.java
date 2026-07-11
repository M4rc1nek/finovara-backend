package com.finovara.reportservice.pdfexport.service.strategy;

import com.finovara.contracts.model.PeriodType;
import com.finovara.reportservice.pdfexport.document.PdfReportDocument;
import com.finovara.reportservice.pdfexport.model.PdfReportType;
import com.finovara.reportservice.pdfexport.service.strategy.label.PdfReportText;
import com.finovara.reportservice.util.dto.ReportDto;
import com.finovara.reportservice.report.finances.calculate.sum.ReportSummaryService;
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
class SumFinancesPdfTest {

    private static final Long USER_ID = 1L;
    private static final PeriodType PERIOD_TYPE = PeriodType.MONTHLY;
    private static final BigDecimal REVENUE_AMOUNT = new BigDecimal("1000");
    private static final BigDecimal EXPENSE_AMOUNT = new BigDecimal("400");
    private static final BigDecimal BALANCE = new BigDecimal("600");

    @Mock
    private ReportSummaryService reportSummaryService;

    @Mock
    private PdfReportDocument document;

    private SumFinancesPdf sumFinancesPdf;

    @BeforeEach
    void setUp() {
        sumFinancesPdf = new SumFinancesPdf(reportSummaryService);
    }

    @Nested
    class GetType {

        @Test
        void shouldReturnSumFinancesType() {
            assertThat(sumFinancesPdf.getType()).isEqualTo(PdfReportType.SUM_FINANCES);
        }
    }

    @Nested
    class GetTitle {

        @Test
        void shouldReturnConstantTitle() {
            assertThat(sumFinancesPdf.getTitle(PERIOD_TYPE)).isEqualTo("Zsumowane przychody i wydatki");
        }
    }

    @Nested
    class GetFileName {

        @Test
        void shouldContainBaseName() {
            assertThat(sumFinancesPdf.getFileName(PERIOD_TYPE)).contains("zsumowane-przychody-wydatki");
        }
    }

    @Nested
    class Generate {

        @ParameterizedTest
        @EnumSource(PeriodType.class)
        void shouldBuildReportFromSummaryService(PeriodType periodType) throws Exception {
            ReportDto revenue = new ReportDto(periodType, REVENUE_AMOUNT);
            ReportDto expense = new ReportDto(periodType, EXPENSE_AMOUNT);

            when(reportSummaryService.sumRevenue(USER_ID, periodType)).thenReturn(revenue);
            when(reportSummaryService.sumExpense(USER_ID, periodType)).thenReturn(expense);
            when(document.formatMoney(REVENUE_AMOUNT)).thenReturn("1000");
            when(document.formatMoney(EXPENSE_AMOUNT)).thenReturn("400");

            sumFinancesPdf.generate(document, USER_ID, periodType);

            verify(document).addSection("Podsumowanie");
            verify(document).addInfo("Okres:", PdfReportText.periodLabel(periodType));
            verify(document).addLineChart(
                    eq("Porównanie przepływu"),
                    eq(List.of("Przychody", "Wydatki", "Bilans")),
                    eq(List.of(REVENUE_AMOUNT, EXPENSE_AMOUNT, BALANCE)),
                    eq(true)
            );
            verify(document).addTable(eq(new String[]{"Typ", "Wartość"}), anyList());
            verify(document).addSummary("Bilans", BALANCE);
        }

        @Test
        void shouldHandleZeroAmounts() throws Exception {
            ReportDto revenue = new ReportDto(PERIOD_TYPE, BigDecimal.ZERO);
            ReportDto expense = new ReportDto(PERIOD_TYPE, BigDecimal.ZERO);

            when(reportSummaryService.sumRevenue(USER_ID, PERIOD_TYPE)).thenReturn(revenue);
            when(reportSummaryService.sumExpense(USER_ID, PERIOD_TYPE)).thenReturn(expense);
            when(document.formatMoney(BigDecimal.ZERO)).thenReturn("0");

            sumFinancesPdf.generate(document, USER_ID, PERIOD_TYPE);

            verify(document).addSummary("Bilans", BigDecimal.ZERO);
            verify(document).addLineChart(
                    eq("Porównanie przepływu"),
                    eq(List.of("Przychody", "Wydatki", "Bilans")),
                    eq(List.of(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO)),
                    eq(true)
            );
        }

        @Test
        void shouldHandleNegativeBalance() throws Exception {
            ReportDto revenue = new ReportDto(PERIOD_TYPE, new BigDecimal("100"));
            ReportDto expense = new ReportDto(PERIOD_TYPE, new BigDecimal("300"));

            when(reportSummaryService.sumRevenue(USER_ID, PERIOD_TYPE)).thenReturn(revenue);
            when(reportSummaryService.sumExpense(USER_ID, PERIOD_TYPE)).thenReturn(expense);
            when(document.formatMoney(new BigDecimal("100"))).thenReturn("100");
            when(document.formatMoney(new BigDecimal("300"))).thenReturn("300");

            sumFinancesPdf.generate(document, USER_ID, PERIOD_TYPE);

            verify(document).addSummary("Bilans", new BigDecimal("-200"));
            verify(document).addLineChart(
                    eq("Porównanie przepływu"),
                    eq(List.of("Przychody", "Wydatki", "Bilans")),
                    eq(List.of(new BigDecimal("100"), new BigDecimal("300"), new BigDecimal("-200"))),
                    eq(true)
            );
        }
    }
}
