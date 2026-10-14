package com.finovara.reportservice.pdfexport;

import com.finovara.contracts.util.PeriodType;
import com.finovara.reportservice.pdfexport.document.PdfReportDocument;
import com.finovara.reportservice.pdfexport.model.PdfReportType;

import java.io.IOException;

public interface ReportPdfHandler {
    PdfReportType getType();

    String getTitle(PeriodType periodType);

    String getFileName(PeriodType periodType);

    void generate(PdfReportDocument document, PdfReportContext context) throws IOException;
}