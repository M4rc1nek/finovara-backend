package com.finovara.finovarabackend.pdfexport.report.service;

import com.finovara.finovarabackend.pdfexport.report.document.PdfReportDocument;
import com.finovara.finovarabackend.pdfexport.report.model.PdfReportType;
import com.finovara.finovarabackend.util.model.PeriodType;

import java.io.IOException;

public interface ReportPdfHandler {
    PdfReportType getType();

    String getTitle(PeriodType periodType);

    String getFileName(PeriodType periodType);

    void generate(PdfReportDocument document, Long userId, PeriodType periodType) throws IOException;
}
