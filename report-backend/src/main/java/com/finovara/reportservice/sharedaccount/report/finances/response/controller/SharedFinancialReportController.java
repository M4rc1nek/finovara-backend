package com.finovara.reportservice.sharedaccount.report.finances.response.controller;

import com.finovara.contracts.model.PeriodType;
import com.finovara.reportservice.util.dto.financial.FinancialReportDto;
import com.finovara.reportservice.security.SecurityUtils;
import com.finovara.reportservice.sharedaccount.report.finances.response.service.SharedFinancialReportService;
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

    private final SharedFinancialReportService financialReportService;

    @GetMapping
    public ResponseEntity<FinancialReportDto> getFinancialReport(@RequestParam Long memberId, @RequestParam PeriodType periodType) {
        return ResponseEntity.ok(financialReportService.getFinancialReport(SecurityUtils.getCurrentUserId(), memberId, periodType));
    }
}