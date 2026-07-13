package com.finovara.reportservice.report.finances.response.controller;

import com.finovara.contracts.model.PeriodType;
import com.finovara.reportservice.util.dto.financial.FinancialReportDto;
import com.finovara.reportservice.report.finances.response.service.FinancialReportService;
import com.finovara.reportservice.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports/financial-report")
@RequiredArgsConstructor
public class FinancialReportController {

    private final FinancialReportService financialReportService;

    @GetMapping
    public ResponseEntity<FinancialReportDto> getFinancialReport(@RequestParam PeriodType periodType) {
        return ResponseEntity.ok(financialReportService.getFinancialReport(SecurityUtils.getCurrentUserId(), periodType));
    }
}

