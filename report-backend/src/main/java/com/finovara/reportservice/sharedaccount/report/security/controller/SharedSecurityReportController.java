package com.finovara.reportservice.sharedaccount.report.security.controller;

import com.finovara.contracts.util.PeriodType;
import com.finovara.contracts.sharedaccount.report.security.dto.SharedAccountSecurityOverviewDto;
import com.finovara.reportservice.security.SecurityUtils;
import com.finovara.reportservice.sharedaccount.report.security.service.SharedSecurityReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/shared-accounts/reports/security-report")
@RequiredArgsConstructor
public class SharedSecurityReportController {

    private final SharedSecurityReportService sharedSecurityReportService;

    @GetMapping
    public ResponseEntity<SharedAccountSecurityOverviewDto> getSharedAccountSecurityReport(@RequestParam PeriodType periodType) {
        return ResponseEntity.ok(sharedSecurityReportService.buildReport(SecurityUtils.getCurrentUserId(), periodType));
    }
}