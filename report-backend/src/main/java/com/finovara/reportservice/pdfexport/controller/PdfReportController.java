package com.finovara.reportservice.pdfexport.controller;

import com.finovara.contracts.util.PeriodType;
import com.finovara.reportservice.pdfexport.PdfExecutionService;
import com.finovara.reportservice.pdfexport.model.PdfReportType;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/pdf/reports")
@RequiredArgsConstructor
public class PdfReportController {
    private final PdfExecutionService pdfExecutionService;

    @GetMapping("/{userId}/download")
    @Operation(summary = "Download PDF report",
            description = "For IMPORTANT_INFORMATION type, periodType parameter is ignored — report always shows monthly, weekly and daily data.")
    public ResponseEntity<byte[]> downloadReport(@PathVariable Long userId, @RequestParam PdfReportType type, @RequestParam PeriodType periodType) {
        byte[] pdf = pdfExecutionService.execute(type, periodType, userId);

        return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF).header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(pdfExecutionService.getFileName(type, periodType))
                        .build()
                        .toString())
                .body(pdf);
    }
}
