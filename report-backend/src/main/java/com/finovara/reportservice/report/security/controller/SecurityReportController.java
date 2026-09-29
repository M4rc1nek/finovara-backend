package com.finovara.reportservice.report.security.controller;

import com.finovara.contracts.model.PeriodType;
import com.finovara.contracts.report.dto.SecurityReportDto;
import com.finovara.reportservice.report.security.service.SecurityReportService;
import com.finovara.reportservice.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports/security-report")
@RequiredArgsConstructor
public class SecurityReportController {
    private  final SecurityReportService securityReportService;

    @GetMapping
    public ResponseEntity<SecurityReportDto> getSecurityReport(@RequestParam PeriodType periodType) {
        return ResponseEntity.ok(securityReportService.buildReport(SecurityUtils.getCurrentUserId(), periodType));
    }
}
