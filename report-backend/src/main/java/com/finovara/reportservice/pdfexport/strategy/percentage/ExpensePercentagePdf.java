package com.finovara.reportservice.pdfexport.strategy.percentage;

import com.finovara.contracts.mainaccount.report.financial.dto.FinancialCategoryPercentageDto;
import com.finovara.contracts.util.PeriodType;
import com.finovara.reportservice.pdfexport.PdfReportContext;
import com.finovara.reportservice.pdfexport.ReportPdfHandler;
import com.finovara.reportservice.pdfexport.document.PdfReportDocument;
import com.finovara.reportservice.pdfexport.model.PdfReportType;
import com.finovara.reportservice.pdfexport.strategy.PdfReportValues;
import com.finovara.reportservice.pdfexport.strategy.label.PdfReportText;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

@Component
public class ExpensePercentagePdf implements ReportPdfHandler {

    @Override
    public PdfReportType getType() {
        return PdfReportType.PERCENTAGE_OF_EXPENSES;
    }

    @Override
    public String getTitle(PeriodType periodType) {
        return "Udział procentowy wydatków";
    }

    @Override
    public String getFileName(PeriodType periodType) {
        return PdfReportText.fileName("udzial-procentowy-wydatkow", periodType);
    }

    @Override
    public void generate(PdfReportDocument document, PdfReportContext context) throws IOException {
        List<FinancialCategoryPercentageDto> percentages =
                PdfReportValues.safe(context.report().expenseCategoryPercentages());

        document.addSection("Udział wydatków według kategorii");
        document.addInfo("Okres:", PdfReportText.periodLabel(context.periodType()));
        document.addPieChart(
                "Struktura wydatków",
                percentages.stream().map(dto -> PdfReportText.expenseCategoryLabel(dto.category())).toList(),
                percentages.stream().map(FinancialCategoryPercentageDto::percentage).toList()
        );
        document.addTable(
                new String[]{"Kategoria", "Udział"},
                percentages.stream()
                        .map(dto -> new String[]{
                                PdfReportText.expenseCategoryLabel(dto.category()),
                                document.formatPercent(dto.percentage())
                        })
                        .toList()
        );
    }
}