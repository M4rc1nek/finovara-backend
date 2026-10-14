package com.finovara.reportservice.sharedaccount.report.finances.response.controller;

import com.finovara.contracts.mainaccount.report.financial.dto.FinancialReportDto;
import com.finovara.contracts.util.PeriodType;
import com.finovara.reportservice.security.SecurityUtils;
import com.finovara.reportservice.sharedaccount.report.finances.response.service.SharedAccountFinancialReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/shared-accounts/reports/financial-report")
@RequiredArgsConstructor
public class SharedFinancialReportController {

    private final SharedAccountFinancialReportService sharedAccountFinancialReportService;

    @GetMapping
    public ResponseEntity<FinancialReportDto> getFinancialReport(@RequestParam Long memberId, @RequestParam PeriodType periodType) {
        return ResponseEntity.ok(sharedAccountFinancialReportService.buildReport(SecurityUtils.getCurrentUserId(), memberId, periodType));
    }
}