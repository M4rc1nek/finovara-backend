package com.finovara.reportservice.pdfexport.service.strategy.highest;

import com.finovara.contracts.model.PeriodType;
import com.finovara.contracts.transaction.report.dto.HighestExpenseDto;
import com.finovara.reportservice.pdfexport.document.PdfReportDocument;
import com.finovara.reportservice.pdfexport.model.PdfReportType;
import com.finovara.reportservice.pdfexport.service.ReportPdfHandler;
import com.finovara.reportservice.pdfexport.service.strategy.label.PdfReportText;
import com.finovara.reportservice.report.finances.calculate.highesttransactions.highestexpense.service.HighestExpenseService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class HighestExpensesPdf implements ReportPdfHandler {

    private final HighestExpenseService highestExpenseService;

    @Override
    public PdfReportType getType() {
        return PdfReportType.HIGHEST_EXPENSES;
    }

    @Override
    public String getTitle(PeriodType periodType) {
        return "Największe wydatki";
    }

    @Override
    public String getFileName(PeriodType periodType) {
        return PdfReportText.fileName("najwieksze-wydatki", periodType);
    }

    @Override
    public void generate(PdfReportDocument document, Long userId, PeriodType periodType) throws IOException {
        List<HighestExpenseDto> expenses = highestExpenseService.getHighestExpense(userId, periodType);

        document.addSection("Największe wydatki");
        document.addInfo("Okres:", PdfReportText.periodLabel(periodType));
        document.addBarChart(
                "Największe wydatki według kategorii",
                expenses.stream()
                        .map(e -> PdfReportText.expenseCategoryLabel(e.category()))
                        .toList(),
                expenses.stream()
                        .map(HighestExpenseDto::amount)
                        .toList(),
                true
        );
        document.addTable(
                new String[]{"Kategoria", "Kwota"},
                expenses.stream()
                        .map(e -> new String[]{
                                PdfReportText.expenseCategoryLabel(e.category()),
                                document.formatMoney(e.amount())
                        })
                        .toList()
        );
    }
}