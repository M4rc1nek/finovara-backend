package com.finovara.reportservice.sharedaccount.report.cache.evict.controller;

import com.finovara.reportservice.sharedaccount.report.cache.evict.service.EvictSharedReportCacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/report-cache")
@RequiredArgsConstructor
public class RefreshReportCacheController {

    private final EvictSharedReportCacheService evictReportCacheService;

    @PostMapping("/refresh")
    public ResponseEntity<Void> refreshReportData(@RequestHeader("X-User-Id") Long userId) {
        evictReportCacheService.evictDataForUser(userId);
        return ResponseEntity.ok().build();
    }
}
