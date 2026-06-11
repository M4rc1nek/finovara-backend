package com.finovara.corebackend.pdfexport.report.service.strategy;

import com.finovara.corebackend.pdfexport.report.document.PdfReportDocument;
import com.finovara.corebackend.pdfexport.report.model.PdfReportType;
import com.finovara.corebackend.pdfexport.report.service.strategy.label.PdfReportText;
import com.finovara.corebackend.report.dto.ReportDto;
import com.finovara.corebackend.report.finances.sum.service.ReportSummaryService;
import com.finovara.contracts.model.PeriodType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SumFinancesPdfTest {

    @Mock
    private ReportSummaryService reportSummaryService;

    @Mock
    private PdfReportDocument document;

    private SumFinancesPdf sumFinancesPdf;

    @BeforeEach
    void setUp() {
        sumFinancesPdf = new SumFinancesPdf(reportSummaryService);
    }

    @Test
    void shouldReturnCorrectType() {
        assertThat(sumFinancesPdf.getType()).isEqualTo(PdfReportType.SUM_FINANCES);
    }

    @Test
    void shouldReturnConstantTitle() {
        assertThat(sumFinancesPdf.getTitle(PeriodType.MONTHLY)).isEqualTo("Zsumowane przychody i wydatki");
    }

    @Test
    void shouldReturnFileNameContainingBaseName() {
        String fileName = sumFinancesPdf.getFileName(PeriodType.MONTHLY);

        assertThat(fileName).contains("zsumowane-przychody-wydatki");
    }

    @Test
    void shouldGenerateReportHappyPath() throws Exception {
        ReportDto revenue = mock(ReportDto.class);
        ReportDto expense = mock(ReportDto.class);

        when(revenue.amount()).thenReturn(new BigDecimal("1000"));
        when(expense.amount()).thenReturn(new BigDecimal("400"));

        when(reportSummaryService.sumRevenue(1L, PeriodType.MONTHLY)).thenReturn(revenue);
        when(reportSummaryService.sumExpense(1L, PeriodType.MONTHLY)).thenReturn(expense);

        when(document.formatMoney(new BigDecimal("1000"))).thenReturn("1000");
        when(document.formatMoney(new BigDecimal("400"))).thenReturn("400");

        sumFinancesPdf.generate(document, 1L, PeriodType.MONTHLY);

        verify(document).addSection("Podsumowanie");
        verify(document).addInfo("Okres:", PdfReportText.periodLabel(PeriodType.MONTHLY));
        verify(document).addLineChart(
                eq("Porównanie przepływu"),
                eq(List.of("Przychody", "Wydatki", "Bilans")),
                eq(List.of(new BigDecimal("1000"), new BigDecimal("400"), new BigDecimal("600"))),
                eq(true)
        );
        verify(document).addTable(eq(new String[]{"Typ", "Wartość"}), anyList());
        verify(document).addSummary("Bilans", new BigDecimal("600"));
    }

    @Test
    void shouldHandleZeroValuesGracefully() throws Exception {
        ReportDto revenue = mock(ReportDto.class);
        ReportDto expense = mock(ReportDto.class);

        when(revenue.amount()).thenReturn(BigDecimal.ZERO);
        when(expense.amount()).thenReturn(BigDecimal.ZERO);

        when(reportSummaryService.sumRevenue(1L, PeriodType.MONTHLY)).thenReturn(revenue);
        when(reportSummaryService.sumExpense(1L, PeriodType.MONTHLY)).thenReturn(expense);

        when(document.formatMoney(BigDecimal.ZERO)).thenReturn("0");

        sumFinancesPdf.generate(document, 1L, PeriodType.MONTHLY);

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
        ReportDto revenue = mock(ReportDto.class);
        ReportDto expense = mock(ReportDto.class);

        when(revenue.amount()).thenReturn(new BigDecimal("100"));
        when(expense.amount()).thenReturn(new BigDecimal("300"));

        when(reportSummaryService.sumRevenue(1L, PeriodType.MONTHLY)).thenReturn(revenue);
        when(reportSummaryService.sumExpense(1L, PeriodType.MONTHLY)).thenReturn(expense);

        when(document.formatMoney(new BigDecimal("100"))).thenReturn("100");
        when(document.formatMoney(new BigDecimal("300"))).thenReturn("300");

        sumFinancesPdf.generate(document, 1L, PeriodType.MONTHLY);

        verify(document).addSummary("Bilans", new BigDecimal("-200"));
        verify(document).addLineChart(
                eq("Porównanie przepływu"),
                eq(List.of("Przychody", "Wydatki", "Bilans")),
                eq(List.of(new BigDecimal("100"), new BigDecimal("300"), new BigDecimal("-200"))),
                eq(true)
        );
    }
}