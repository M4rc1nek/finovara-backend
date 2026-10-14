package com.finovara.reportservice.pdfexport.strategy.highest;

import com.finovara.contracts.mainaccount.report.financial.dto.HighestRevenueDto;
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
public class HighestRevenuesPdf implements ReportPdfHandler {

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
    public void generate(PdfReportDocument document, PdfReportContext context) throws IOException {
        List<HighestRevenueDto> revenues = PdfReportValues.safe(context.report().highestRevenues());

        document.addSection("Największe przychody");
        document.addInfo("Okres:", PdfReportText.periodLabel(context.periodType()));
        document.addBarChart(
                "Największe przychody według kategorii",
                revenues.stream().map(r -> PdfReportText.revenueCategoryLabel(r.category())).toList(),
                revenues.stream().map(HighestRevenueDto::amount).toList(),
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