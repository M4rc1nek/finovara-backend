package com.finovara.reportservice.pdfexport.service.strategy.percentage;

import com.finovara.contracts.model.PeriodType;
import com.finovara.contracts.model.transaction.RevenueCategory;
import com.finovara.reportservice.pdfexport.document.PdfReportDocument;
import com.finovara.reportservice.pdfexport.model.PdfReportType;
import com.finovara.reportservice.pdfexport.service.ReportPdfHandler;
import com.finovara.reportservice.pdfexport.service.strategy.label.PdfReportText;
import com.finovara.reportservice.report.finances.calculate.categorypercentage.revenue.dto.RevenueCategoryPercentageDto;
import com.finovara.reportservice.report.finances.calculate.categorypercentage.revenue.service.RevenueCategoryPercentageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
public class RevenuePercentagePdf implements ReportPdfHandler {

    private final RevenueCategoryPercentageService revenueCategoryPercentageService;

    @Override
    public PdfReportType getType() {
        return PdfReportType.PERCENTAGE_OF_REVENUES;
    }

    @Override
    public String getTitle(PeriodType periodType) {
        return "Udział procentowy przychodów";
    }

    @Override
    public String getFileName(PeriodType periodType) {
        return PdfReportText.fileName("udzial-procentowy-przychodow", periodType);
    }

    @Override
    public void generate(PdfReportDocument document, Long userId, PeriodType periodType) throws IOException {
        List<RevenueCategoryPercentageDto> percentages = Arrays.stream(RevenueCategory.values())
                .map(cat -> revenueCategoryPercentageService
                        .getRevenuePercentageByCategoryReport(userId, cat, periodType))
                .toList();

        document.addSection("Udział przychodów według kategorii");
        document.addInfo("Okres:", PdfReportText.periodLabel(periodType));
        document.addPieChart(
                "Struktura przychodów",
                percentages.stream()
                        .map(dto -> PdfReportText.revenueCategoryLabel(dto.category()))
                        .toList(),
                percentages.stream()
                        .map(RevenueCategoryPercentageDto::percentage)
                        .toList()
        );
        document.addTable(
                new String[]{"Kategoria", "Udział"},
                percentages.stream()
                        .map(dto -> new String[]{
                                PdfReportText.revenueCategoryLabel(dto.category()),
                                document.formatPercent(dto.percentage())
                        })
                        .toList()
        );
    }
}