package com.finovara.finovarabackend.pdfexport.report.service.strategy.percentage;

import com.finovara.finovarabackend.expense.model.ExpenseCategory;
import com.finovara.finovarabackend.pdfexport.report.document.PdfReportDocument;
import com.finovara.finovarabackend.pdfexport.report.model.PdfReportType;
import com.finovara.finovarabackend.pdfexport.report.service.ReportPdfHandler;
import com.finovara.finovarabackend.pdfexport.report.service.strategy.label.PdfReportText;
import com.finovara.finovarabackend.report.finances.categorypercentage.expense.dto.ExpenseCategoryPercentageDto;
import com.finovara.finovarabackend.report.finances.categorypercentage.expense.service.ExpenseCategoryPercentageService;
import com.finovara.finovarabackend.util.model.PeriodType;
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
                .map(category -> expenseCategoryPercentageService.getExpensePercentageByCategoryReport(userId, category, periodType))
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
        document.addTable(new String[]{"Kategoria", "Udział"},
                percentages.stream()
                        .map(dto -> new String[]{
                                PdfReportText.expenseCategoryLabel(dto.category()),
                                document.formatPercent(dto.percentage())
                        })
                        .toList()
        );
    }
}
