package com.finovara.reportservice.pdfexport.service.strategy.percentage;

import com.finovara.contracts.model.transaction.ExpenseCategory;
import com.finovara.contracts.model.PeriodType;
import com.finovara.reportservice.pdfexport.document.PdfReportDocument;
import com.finovara.reportservice.pdfexport.model.PdfReportType;
import com.finovara.reportservice.pdfexport.service.ReportPdfHandler;
import com.finovara.reportservice.pdfexport.service.strategy.label.PdfReportText;
import com.finovara.reportservice.report.finances.categorypercentage.expense.dto.ExpenseCategoryPercentageDto;
import com.finovara.reportservice.report.finances.categorypercentage.expense.service.ExpenseCategoryPercentageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ExpensePercentagePdf implements ReportPdfHandler {

    private final ExpenseCategoryPercentageService expenseCategoryPercentageService;

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
    public void generate(PdfReportDocument document, Long userId, PeriodType periodType) throws IOException {
        List<ExpenseCategoryPercentageDto> percentages = Arrays.stream(ExpenseCategory.values())
                .map(cat -> expenseCategoryPercentageService
                        .getExpensePercentageByCategoryReport(userId, cat, periodType))
                .toList();

        document.addSection("Udział wydatków według kategorii");
        document.addInfo("Okres:", PdfReportText.periodLabel(periodType));
        document.addPieChart(
                "Struktura wydatków",
                percentages.stream()
                        .map(dto -> PdfReportText.expenseCategoryLabel(dto.category()))
                        .toList(),
                percentages.stream()
                        .map(ExpenseCategoryPercentageDto::percentage)
                        .toList()
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