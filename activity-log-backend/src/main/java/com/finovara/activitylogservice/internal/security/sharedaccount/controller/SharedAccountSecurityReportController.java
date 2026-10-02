package com.finovara.activitylogservice.internal.security.sharedaccount.controller;

import com.finovara.activitylogservice.internal.security.sharedaccount.service.SharedAccountSecurityReportService;
import com.finovara.contracts.util.PeriodType;
import com.finovara.contracts.sharedaccount.report.security.dto.SharedAccountSecurityOverviewDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal/shared-accounts/reports/security")
@RequiredArgsConstructor
public class SharedAccountSecurityReportController {

    private final SharedAccountSecurityReportService sharedAccountSecurityReportService;

    @GetMapping
    public ResponseEntity<SharedAccountSecurityOverviewDto> getSharedAccountSecurityOverview(@RequestHeader("X-User-Id") Long userId, @RequestParam PeriodType periodType) {
        return ResponseEntity.ok(sharedAccountSecurityReportService.getSecurityOverview(userId, periodType));
    }
}