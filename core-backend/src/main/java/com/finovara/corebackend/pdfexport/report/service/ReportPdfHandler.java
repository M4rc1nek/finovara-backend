package com.finovara.corebackend.pdfexport.report.service;

import com.finovara.corebackend.pdfexport.report.document.PdfReportDocument;
import com.finovara.corebackend.pdfexport.report.model.PdfReportType;
import com.finovara.activityservice.contracts.model.PeriodType;

import java.io.IOException;

public interface ReportPdfHandler {
    PdfReportType getType();

    String getTitle(PeriodType periodType);

    String getFileName(PeriodType periodType);

    void generate(PdfReportDocument document, Long userId, PeriodType periodType) throws IOException;
}
