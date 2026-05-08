package com.finovara.finovarabackend.pdfexport.report.service.strategy;

import com.finovara.finovarabackend.pdfexport.report.document.PdfReportDocument;
import com.finovara.finovarabackend.pdfexport.report.model.PdfReportType;
import com.finovara.finovarabackend.pdfexport.report.service.strategy.label.PdfReportText;
import com.finovara.finovarabackend.report.dto.ReportDto;
import com.finovara.finovarabackend.report.finances.sum.service.ReportSummaryService;
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
class SumFinancesPdfStrategyTest {

    @Mock
    private ReportSummaryService reportSummaryService;

    @Mock
    private PdfReportDocument document;

    private SumFinancesPdfStrategy sumFinancesPdfStrategy;

    @BeforeEach
    void setUp() {
        sumFinancesPdfStrategy = new SumFinancesPdfStrategy(reportSummaryService);
    }

    @Test
    void shouldReturnCorrectType() {
        assertThat(sumFinancesPdfStrategy.getType()).isEqualTo(PdfReportType.SUM_FINANCES);
    }

    @Test
    void shouldReturnConstantTitle() {
        assertThat(sumFinancesPdfStrategy.getTitle(PeriodType.MONTHLY)).isEqualTo("Zsumowane przychody i wydatki");
    }

    @Test
    void shouldReturnFileNameContainingBaseName() {
        String fileName = sumFinancesPdfStrategy.getFileName(PeriodType.MONTHLY);

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

        sumFinancesPdfStrategy.generate(document, 1L, PeriodType.MONTHLY);

        verify(document).addSection("Podsumowanie");
        verify(document).addInfo("Okres:", PdfReportText.periodLabel(PeriodType.MONTHLY));
        verify(document).addLineChart(eq("Porównanie przepływu"), eq(List.of("Przychody", "Wydatki")), anyList(), eq(true));
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

        sumFinancesPdfStrategy.generate(document, 1L, PeriodType.MONTHLY);

        verify(document).addSummary("Bilans", BigDecimal.ZERO);
        verify(document).addLineChart(eq("Porównanie przepływu"), anyList(), anyList(), eq(true));
    }

    @Test
    void shouldHandleNegativeBalance() throws Exception {
        ReportDto revenue = mock(ReportDto.class);
        ReportDto expense = mock(ReportDto.class);

        when(revenue.amount()).thenReturn(new BigDecimal("100"));
        when(expense.amount()).thenReturn(new BigDecimal("300"));

        when(reportSummaryService.sumRevenue(1L, PeriodType.MONTHLY)).thenReturn(revenue);
        when(reportSummaryService.sumExpense(1L, PeriodType.MONTHLY)).thenReturn(expense);

        sumFinancesPdfStrategy.generate(document, 1L, PeriodType.MONTHLY);

        verify(document).addSummary("Bilans", new BigDecimal("-200"));
    }
}