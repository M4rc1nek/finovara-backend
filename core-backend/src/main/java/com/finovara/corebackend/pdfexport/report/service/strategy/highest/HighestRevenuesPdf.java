package com.finovara.corebackend.pdfexport.report.service.strategy.highest;

import com.finovara.corebackend.pdfexport.report.model.PdfReportType;
import com.finovara.corebackend.pdfexport.report.service.ReportPdfHandler;
import com.finovara.corebackend.pdfexport.report.document.PdfReportDocument;
import com.finovara.corebackend.pdfexport.report.service.strategy.label.PdfReportText;
import com.finovara.corebackend.report.finances.highesttransactions.highestrevenue.dto.HighestRevenueDto;
import com.finovara.corebackend.report.finances.highesttransactions.highestrevenue.service.HighestRevenueService;
import com.finovara.contracts.model.PeriodType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class HighestRevenuesPdf implements ReportPdfHandler {
    private final HighestRevenueService highestRevenueService;

    @Override
    public PdfReportType getType() {
        return PdfReportType.HIGHEST_REVENUES;
    }

    @Override
    public String getTitle(PeriodType periodType) {
        return "Największe przychody";
    }

    @Override
    public String getFileName(PeriodType periodType) {
        return PdfReportText.fileName("najwieksze-przychody", periodType);
    }

    @Override
    public void generate(PdfReportDocument document, Long userId, PeriodType periodType) throws IOException {
        List<HighestRevenueDto> revenues = highestRevenueService.getHighestRevenue(userId, periodType);

        document.addSection("Największe przychody");
        document.addInfo("Okres:", PdfReportText.periodLabel(periodType));
        document.addBarChart(
                "Największe przychody według kategorii",
                revenues.stream()
                        .map(revenue -> PdfReportText.revenueCategoryLabel(revenue.category()))
                        .toList(),
                revenues.stream()
                        .map(HighestRevenueDto::amount)
                        .toList(),
                true
        );
        document.addTable(
                new String[]{"Kategoria", "Kwota"},
                revenues.stream()
                        .map(revenue -> new String[]{
                                PdfReportText.revenueCategoryLabel(revenue.category()),
                                document.formatMoney(revenue.amount())
                        })
                        .toList()
        );
    }
}
