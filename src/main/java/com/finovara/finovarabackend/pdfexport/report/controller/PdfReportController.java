package com.finovara.finovarabackend.pdfexport.report.controller;

import com.finovara.finovarabackend.pdfexport.report.model.PdfReportType;
import com.finovara.finovarabackend.pdfexport.report.service.PdfExecutionService;
import com.finovara.finovarabackend.util.model.PeriodType;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pdf/reports")
@RequiredArgsConstructor
public class PdfReportController {
    private final PdfExecutionService pdfExecutionService;

    @GetMapping("/{userId}/download")
    public ResponseEntity<byte[]> downloadReport(@PathVariable Long userId, @RequestParam PdfReportType type, @RequestParam PeriodType periodType) {
        byte[] pdf = pdfExecutionService.execute(type, periodType, userId);

        return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF).header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(pdfExecutionService.getFileName(type, periodType))
                        .build()
                        .toString())
                .body(pdf);
    }
}
