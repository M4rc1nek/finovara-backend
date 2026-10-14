package com.finovara.reportservice.pdfexport.strategy;

import com.finovara.contracts.mainaccount.report.financial.dto.FinancialReportDto;
import com.finovara.contracts.util.PeriodType;
import com.finovara.reportservice.pdfexport.PdfReportContext;
import com.finovara.reportservice.pdfexport.ReportPdfHandler;
import com.finovara.reportservice.pdfexport.document.PdfReportDocument;
import com.finovara.reportservice.pdfexport.model.PdfReportType;
import com.finovara.reportservice.pdfexport.strategy.label.PdfReportText;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

@Component
public class SumFinancesPdf implements ReportPdfHandler {

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
    public void generate(PdfReportDocument document, PdfReportContext context) throws IOException {
        FinancialReportDto report = context.report();
        BigDecimal revenue = PdfReportValues.revenues(report);
        BigDecimal expense = PdfReportValues.expenses(report);
        BigDecimal balance = revenue.subtract(expense);

        document.addSection("Podsumowanie");
        document.addInfo("Okres:", PdfReportText.periodLabel(context.periodType()));
        document.addLineChart(
                "Porównanie przepływu",
                List.of("Przychody", "Wydatki", "Bilans"),
                List.of(revenue, expense, balance),
                true
        );
        document.addTable(
                new String[]{"Typ", "Wartość"},
                List.of(
                        new String[]{"Przychody", document.formatMoney(revenue)},
                        new String[]{"Wydatki", document.formatMoney(expense)}
                )
        );
        document.addSummary("Bilans", balance);
    }
}