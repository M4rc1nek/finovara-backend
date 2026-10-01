package com.finovara.reportservice.sharedaccount.report.cache.evict.controller;

import com.finovara.reportservice.security.SecurityUtils;
import com.finovara.reportservice.sharedaccount.report.cache.evict.service.EvictSharedReportCacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/shared-accounts/report-cache")
@RequiredArgsConstructor
public class RefreshSharedReportCacheController {

    private final EvictSharedReportCacheService evictReportCacheService;

    @PostMapping("/refresh-financial")
    public ResponseEntity<Void> refreshSharedReportData() {
        evictReportCacheService.evictDataForUser(SecurityUtils.getCurrentUserId());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/refresh-security")
    public ResponseEntity<Void> refreshSharedAccountSecurityReportData(@RequestHeader("X-User-Id") Long userId) {
        evictReportCacheService.evictSharedAccountSecurityDataForUser(userId);
        return ResponseEntity.ok().build();
    }
}
