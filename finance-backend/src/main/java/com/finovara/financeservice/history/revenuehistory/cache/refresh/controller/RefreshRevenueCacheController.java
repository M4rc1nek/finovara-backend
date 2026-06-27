package com.finovara.financeservice.history.revenuehistory.cache.refresh.controller;

import com.finovara.financeservice.history.revenuehistory.cache.refresh.service.RefreshCacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/revenue-history-cache")
@RequiredArgsConstructor
public class RefreshCacheController {

    private final RefreshCacheService refreshCacheService;

    @PostMapping("/refresh")
    public ResponseEntity<Void> refreshRevenueHistoryData(@RequestHeader("X-User-Id") Long userId) {
        refreshCacheService.evictDataForUser(userId);
        return ResponseEntity.ok().build();
    }
}
