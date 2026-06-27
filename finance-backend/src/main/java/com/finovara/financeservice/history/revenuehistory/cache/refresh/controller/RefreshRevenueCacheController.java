package com.finovara.financeservice.history.revenuehistory.cache.refresh.controller;

import com.finovara.financeservice.history.revenuehistory.cache.refresh.service.RefreshRevenueCacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/revenue-history-cache")
@RequiredArgsConstructor
public class RefreshRevenueCacheController {

    private final RefreshRevenueCacheService refreshRevenueCacheService;

    @PostMapping("/refresh")
    public ResponseEntity<Void> refreshRevenueHistoryData(@RequestHeader("X-User-Id") Long userId) {
        refreshRevenueCacheService.evictDataForUser(userId);
        return ResponseEntity.ok().build();
    }
}
