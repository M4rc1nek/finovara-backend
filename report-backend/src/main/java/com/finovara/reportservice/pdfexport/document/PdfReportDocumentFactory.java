package com.finovara.reportservice.pdfexport.document;

import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class PdfReportDocumentFactory {

    public PdfReportDocument create(String title) throws IOException {
        return new PdfReportDocument(title);
    }
}
