package com.finovara.corebackend.pdfexport.report.service.strategy;

import com.finovara.corebackend.pdfexport.report.model.PdfReportType;
import com.finovara.corebackend.pdfexport.report.service.ReportPdfHandler;
import com.finovara.corebackend.pdfexport.report.document.PdfReportDocument;
import com.finovara.corebackend.pdfexport.report.service.strategy.label.PdfReportText;
import com.finovara.corebackend.report.dto.ReportDto;
import com.finovara.corebackend.report.finances.average.service.ReportAverageService;
import com.finovara.contracts.model.PeriodType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

@Component
@RequiredArgsConstructor
public class AverageFinancesPdf implements ReportPdfHandler {
    private final ReportAverageService reportAverageService;

    @Override
    public PdfReportType getType() {
        return PdfReportType.AVERAGE_FINANCES;
    }

    @Override
    public String getTitle(PeriodType periodType) {
        return "Średnie przychody i wydatki";
    }

    @Override
    public String getFileName(PeriodType periodType) {
        return PdfReportText.fileName("srednie-przychody-wydatki", periodType);
    }

    @Override
    public void generate(PdfReportDocument document, Long userId, PeriodType periodType) throws IOException {
        ReportDto revenue = reportAverageService.calculateAverageRevenue(userId, periodType);
        ReportDto expense = reportAverageService.calculateAverageExpense(userId, periodType);
        BigDecimal difference = revenue.amount().subtract(expense.amount());

        document.addSection("Średnie wartości");
        document.addInfo("Okres:", PdfReportText.periodLabel(periodType));
        document.addLineChart("Relacja średnich wartości", List.of("Przychody", "Wydatki", "Różnica średnich"), List.of(revenue.amount(), expense.amount(), difference), true);
        document.addTable(
                new String[]{"Typ", "Średnia wartość"},
                List.of(
                        new String[]{"Przychody", document.formatMoney(revenue.amount())},
                        new String[]{"Wydatki", document.formatMoney(expense.amount())}
                )
        );
        document.addSummary("Różnica średnich", difference);
    }
}
