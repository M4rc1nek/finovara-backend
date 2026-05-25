package com.finovara.corebackend.pdfexport.report.service.strategy.percentage;

import com.finovara.corebackend.pdfexport.report.model.PdfReportType;
import com.finovara.corebackend.pdfexport.report.service.ReportPdfHandler;
import com.finovara.corebackend.pdfexport.report.document.PdfReportDocument;
import com.finovara.corebackend.pdfexport.report.service.strategy.label.PdfReportText;
import com.finovara.corebackend.report.finances.categorypercentage.revenue.dto.RevenueCategoryPercentageDto;
import com.finovara.corebackend.report.finances.categorypercentage.revenue.service.RevenueCategoryPercentageService;
import com.finovara.activityservice.contracts.model.transaction.RevenueCategory;
import com.finovara.activityservice.contracts.model.PeriodType;
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
                .map(category -> revenueCategoryPercentageService.getRevenuePercentageByCategoryReport(userId, category, periodType))
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
