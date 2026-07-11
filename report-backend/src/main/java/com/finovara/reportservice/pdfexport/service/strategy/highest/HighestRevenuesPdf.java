package com.finovara.reportservice.pdfexport.service.strategy.highest;

import com.finovara.contracts.model.PeriodType;
import com.finovara.contracts.transaction.report.dto.HighestRevenueDto;
import com.finovara.reportservice.pdfexport.document.PdfReportDocument;
import com.finovara.reportservice.pdfexport.model.PdfReportType;
import com.finovara.reportservice.pdfexport.service.ReportPdfHandler;
import com.finovara.reportservice.pdfexport.service.strategy.label.PdfReportText;
import com.finovara.reportservice.report.finances.calculate.highesttransactions.highestrevenue.service.HighestRevenueService;
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
                        .map(r -> PdfReportText.revenueCategoryLabel(r.category()))
                        .toList(),
                revenues.stream()
                        .map(HighestRevenueDto::amount)
                        .toList(),
                true
        );
        document.addTable(
                new String[]{"Kategoria", "Kwota"},
                revenues.stream()
                        .map(r -> new String[]{
                                PdfReportText.revenueCategoryLabel(r.category()),
                                document.formatMoney(r.amount())
                        })
                        .toList()
        );
    }
}