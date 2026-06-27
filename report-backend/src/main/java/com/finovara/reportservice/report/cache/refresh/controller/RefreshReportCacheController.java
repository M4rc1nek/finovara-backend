package com.finovara.reportservice.report.cache.refresh.controller;

import com.finovara.reportservice.report.cache.refresh.service.RefreshCacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/report-cache")
@RequiredArgsConstructor
public class RefreshCacheController {

    private final RefreshCacheService refreshCacheService;

    @PostMapping("/refresh")
    public ResponseEntity<Void> refreshReportData(@RequestHeader("X-User-Id") Long userId) {
        refreshCacheService.evictDataForUser(userId);
        return ResponseEntity.ok().build();
    }
}
