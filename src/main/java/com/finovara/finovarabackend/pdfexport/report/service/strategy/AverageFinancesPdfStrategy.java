package com.finovara.finovarabackend.pdfexport.report.service.strategy;

import com.finovara.finovarabackend.pdfexport.report.model.PdfReportType;
import com.finovara.finovarabackend.pdfexport.report.service.ReportPdfStrategy;
import com.finovara.finovarabackend.pdfexport.report.document.PdfReportDocument;
import com.finovara.finovarabackend.pdfexport.report.service.strategy.label.PdfReportText;
import com.finovara.finovarabackend.report.dto.ReportDto;
import com.finovara.finovarabackend.report.finances.average.service.ReportAverageService;
import com.finovara.finovarabackend.util.model.PeriodType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

@Component
@RequiredArgsConstructor
public class AverageFinancesPdfStrategy implements ReportPdfStrategy {
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
        document.addLineChart("Relacja średnich wartości", List.of("Przychody", "Wydatki"), List.of(revenue.amount(), expense.amount()), true);
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
