package com.finovara.corebackend.pdfexport.report.service;

import com.finovara.contracts.exception.badrequest.InvalidInputException;
import com.finovara.corebackend.pdfexport.report.model.PdfReportType;
import com.finovara.corebackend.pdfexport.report.document.PdfReportDocument;
import com.finovara.corebackend.pdfexport.report.document.PdfReportDocumentFactory;
import com.finovara.contracts.model.PeriodType;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
public class PdfExecutionService {
    private final PdfReportDocumentFactory documentFactory;
    private final Map<PdfReportType, ReportPdfHandler> strategies;

    public PdfExecutionService(PdfReportDocumentFactory documentFactory, List<ReportPdfHandler> strategies) {
        this.documentFactory = documentFactory;
        this.strategies = new EnumMap<>(PdfReportType.class);
        strategies.forEach(strategy -> this.strategies.put(strategy.getType(), strategy));
    }

    public byte[] execute(PdfReportType type, PeriodType periodType, Long userId) {
        validatePeriodType(periodType);
        ReportPdfHandler strategy = getStrategy(type);

        try (PdfReportDocument document = documentFactory.create(strategy.getTitle(periodType))) {
            strategy.generate(document, userId, periodType);
            return document.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Could not generate PDF report.", exception);
        }
    }

    public String getFileName(PdfReportType type, PeriodType periodType) {
        validatePeriodType(periodType);
        return getStrategy(type).getFileName(periodType);
    }

    private void validatePeriodType(PeriodType periodType) {
        if (periodType == null) {
            throw new InvalidInputException("Report period type is required.");
        }
    }

    private ReportPdfHandler getStrategy(PdfReportType type) {
        if (type == null) {
            throw new InvalidInputException("Report type is required.");
        }

        ReportPdfHandler strategy = strategies.get(type);
        if (strategy == null) {
            throw new InvalidInputException("Unsupported PDF report type: " + type);
        }
        return strategy;
    }
}
