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
public class AverageFinancesPdf implements ReportPdfHandler {

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
    public void generate(PdfReportDocument document, PdfReportContext context) throws IOException {
        FinancialReportDto report = context.report();
        PeriodType period = context.periodType();

        BigDecimal revenue = PdfReportValues.averagePerDay(PdfReportValues.revenues(report), period);
        BigDecimal expense = PdfReportValues.averagePerDay(PdfReportValues.expenses(report), period);
        BigDecimal difference = revenue.subtract(expense);

        document.addSection("Średnie wartości");
        document.addInfo("Okres:", PdfReportText.periodLabel(period));
        document.addLineChart(
                "Relacja średnich wartości",
                List.of("Przychody", "Wydatki", "Różnica średnich"),
                List.of(revenue, expense, difference),
                true
        );
        document.addTable(
                new String[]{"Typ", "Średnia dzienna wartość"},
                List.of(
                        new String[]{"Przychody", document.formatMoney(revenue)},
                        new String[]{"Wydatki", document.formatMoney(expense)}
                )
        );
        document.addSummary("Różnica średnich", difference);
    }
}