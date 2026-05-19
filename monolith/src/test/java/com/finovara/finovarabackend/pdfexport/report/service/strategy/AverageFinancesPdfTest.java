package com.finovara.finovarabackend.pdfexport.report.service.strategy;

import com.finovara.finovarabackend.pdfexport.report.document.PdfReportDocument;
import com.finovara.finovarabackend.pdfexport.report.model.PdfReportType;
import com.finovara.finovarabackend.pdfexport.report.service.strategy.label.PdfReportText;
import com.finovara.finovarabackend.report.dto.ReportDto;
import com.finovara.finovarabackend.report.finances.average.service.ReportAverageService;
import com.finovara.finovarabackend.util.model.PeriodType;
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
class AverageFinancesPdfTest {

    @Mock
    private ReportAverageService reportAverageService;

    @Mock
    private PdfReportDocument document;

    private AverageFinancesPdf averageFinancesPdf;

    @BeforeEach
    void setUp() {
        averageFinancesPdf = new AverageFinancesPdf(reportAverageService);
    }

    @Test
    void shouldReturnCorrectType() {
        assertThat(averageFinancesPdf.getType()).isEqualTo(PdfReportType.AVERAGE_FINANCES);
    }

    @Test
    void shouldReturnConstantTitle() {
        assertThat(averageFinancesPdf.getTitle(PeriodType.MONTHLY)).isEqualTo("Średnie przychody i wydatki");
    }

    @Test
    void shouldReturnFileNameContainingBaseName() {
        String fileName = averageFinancesPdf.getFileName(PeriodType.MONTHLY);

        assertThat(fileName).contains("srednie-przychody-wydatki");
    }

    @Test
    void shouldGenerateReportHappyPath() throws Exception {
        ReportDto revenue = mock(ReportDto.class);
        ReportDto expense = mock(ReportDto.class);

        when(revenue.amount()).thenReturn(new BigDecimal("1000"));
        when(expense.amount()).thenReturn(new BigDecimal("400"));

        when(reportAverageService.calculateAverageRevenue(1L, PeriodType.MONTHLY)).thenReturn(revenue);
        when(reportAverageService.calculateAverageExpense(1L, PeriodType.MONTHLY)).thenReturn(expense);

        when(document.formatMoney(new BigDecimal("1000"))).thenReturn("1000");
        when(document.formatMoney(new BigDecimal("400"))).thenReturn("400");

        averageFinancesPdf.generate(document, 1L, PeriodType.MONTHLY);

        verify(document).addSection("Średnie wartości");
        verify(document).addInfo("Okres:", PdfReportText.periodLabel(PeriodType.MONTHLY));
        verify(document).addLineChart(
                eq("Relacja średnich wartości"),
                eq(List.of("Przychody", "Wydatki", "Różnica średnich")),
                eq(List.of(new BigDecimal("1000"), new BigDecimal("400"), new BigDecimal("600"))),
                eq(true)
        );
        verify(document).addTable(eq(new String[]{"Typ", "Średnia wartość"}), anyList());
        verify(document).addSummary("Różnica średnich", new BigDecimal("600"));
    }

    @Test
    void shouldHandleZeroValues() throws Exception {
        ReportDto revenue = mock(ReportDto.class);
        ReportDto expense = mock(ReportDto.class);

        when(revenue.amount()).thenReturn(BigDecimal.ZERO);
        when(expense.amount()).thenReturn(BigDecimal.ZERO);

        when(reportAverageService.calculateAverageRevenue(1L, PeriodType.MONTHLY)).thenReturn(revenue);
        when(reportAverageService.calculateAverageExpense(1L, PeriodType.MONTHLY)).thenReturn(expense);

        when(document.formatMoney(BigDecimal.ZERO)).thenReturn("0");

        averageFinancesPdf.generate(document, 1L, PeriodType.MONTHLY);

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
        ReportDto revenue = mock(ReportDto.class);
        ReportDto expense = mock(ReportDto.class);

        when(revenue.amount()).thenReturn(new BigDecimal("100"));
        when(expense.amount()).thenReturn(new BigDecimal("300"));

        when(reportAverageService.calculateAverageRevenue(1L, PeriodType.MONTHLY)).thenReturn(revenue);
        when(reportAverageService.calculateAverageExpense(1L, PeriodType.MONTHLY)).thenReturn(expense);

        when(document.formatMoney(new BigDecimal("100"))).thenReturn("100");
        when(document.formatMoney(new BigDecimal("300"))).thenReturn("300");

        averageFinancesPdf.generate(document, 1L, PeriodType.MONTHLY);

        verify(document).addSummary("Różnica średnich", new BigDecimal("-200"));
        verify(document).addLineChart(
                eq("Relacja średnich wartości"),
                eq(List.of("Przychody", "Wydatki", "Różnica średnich")),
                eq(List.of(new BigDecimal("100"), new BigDecimal("300"), new BigDecimal("-200"))),
                eq(true)
        );
    }
}