package com.finovara.reportservice.pdfexport.service;

import com.finovara.reportservice.pdfexport.document.PdfReportDocument;
import com.finovara.reportservice.pdfexport.model.PdfReportType;
import com.finovara.contracts.model.PeriodType;

import java.io.IOException;

public interface ReportPdfHandler {
    PdfReportType getType();

    String getTitle(PeriodType periodType);

    String getFileName(PeriodType periodType);

    void generate(PdfReportDocument document, Long userId, PeriodType periodType) throws IOException;
}
