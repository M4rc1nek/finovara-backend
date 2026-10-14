package com.finovara.reportservice.report.cache.evict.controller;

import com.finovara.reportservice.report.cache.evict.service.EvictReportCacheService;
import com.finovara.reportservice.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/report-cache")
@RequiredArgsConstructor
public class RefreshReportCacheController {

    private final EvictReportCacheService evictReportCacheService;

    @PostMapping("/refresh-financial")
    public ResponseEntity<Void> refreshFinancialReportData() {
        evictReportCacheService.evictFinancialDataForUser(SecurityUtils.getCurrentUserId());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/refresh-security")
    public ResponseEntity<Void> refreshSecurityReportData() {
        evictReportCacheService.evictSecurityDataForUser(SecurityUtils.getCurrentUserId());
        return ResponseEntity.ok().build();
    }
}
