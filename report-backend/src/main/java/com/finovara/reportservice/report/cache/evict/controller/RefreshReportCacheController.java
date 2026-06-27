package com.finovara.reportservice.report.cache.evict.controller;

import com.finovara.reportservice.report.cache.evict.service.EvictReportCacheService;
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

    private final EvictReportCacheService evictReportCacheService;

    @PostMapping("/refresh")
    public ResponseEntity<Void> refreshReportData(@RequestHeader("X-User-Id") Long userId) {
        evictReportCacheService.evictDataForUser(userId);
        return ResponseEntity.ok().build();
    }
}
