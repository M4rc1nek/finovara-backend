package com.finovara.reportservice.pdfexport.service.strategy;

import com.finovara.contracts.model.PeriodType;
import com.finovara.reportservice.pdfexport.document.PdfReportDocument;
import com.finovara.reportservice.pdfexport.model.PdfReportType;
import com.finovara.reportservice.pdfexport.service.ReportPdfHandler;
import com.finovara.reportservice.pdfexport.service.strategy.label.PdfReportText;
import com.finovara.reportservice.report.dto.ReportDto;
import com.finovara.reportservice.report.finances.sum.service.ReportSummaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

@Component
@RequiredArgsConstructor
public class SumFinancesPdf implements ReportPdfHandler {

    private final ReportSummaryService reportSummaryService;

    @Override
    public PdfReportType getType() {
        return PdfReportType.SUM_FINANCES;
    }

    @Override
    public String getTitle(PeriodType periodType) {
        return "Zsumowane przychody i wydatki";
    }

    @Override
    public String getFileName(PeriodType periodType) {
        return PdfReportText.fileName("zsumowane-przychody-wydatki", periodType);
    }

    @Override
    public void generate(PdfReportDocument document, Long userId, PeriodType periodType) throws IOException {
        ReportDto revenue = reportSummaryService.sumRevenue(userId, periodType);
        ReportDto expense = reportSummaryService.sumExpense(userId, periodType);
        BigDecimal balance = revenue.amount().subtract(expense.amount());

        document.addSection("Podsumowanie");
        document.addInfo("Okres:", PdfReportText.periodLabel(periodType));
        document.addLineChart(
                "Porównanie przepływu",
                List.of("Przychody", "Wydatki", "Bilans"),
                List.of(revenue.amount(), expense.amount(), balance),
                true
        );
        document.addTable(
                new String[]{"Typ", "Wartość"},
                List.of(
                        new String[]{"Przychody", document.formatMoney(revenue.amount())},
                        new String[]{"Wydatki", document.formatMoney(expense.amount())}
                )
        );
        document.addSummary("Bilans", balance);
    }
}