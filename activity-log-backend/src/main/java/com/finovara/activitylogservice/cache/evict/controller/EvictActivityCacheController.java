package com.finovara.activitylogservice.cache.evict.controller;

import com.finovara.activitylogservice.cache.evict.ActivityCache;
import com.finovara.activitylogservice.cache.evict.EvictActivityCacheService;
import com.finovara.activitylogservice.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/activity-log/refresh")
@RequiredArgsConstructor
public class EvictActivityCacheController {

    private final EvictActivityCacheService evictService;

    @PostMapping("/login")
    public ResponseEntity<Void> refreshLogin() {
        evictService.evict(ActivityCache.LOGIN, SecurityUtils.getCurrentUserId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/login-archive")
    public ResponseEntity<Void> refreshLoginArchive() {
        evictService.evict(ActivityCache.LOGIN_ARCHIVE, SecurityUtils.getCurrentUserId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/account-change")
    public ResponseEntity<Void> refreshAccountChange() {
        evictService.evict(ActivityCache.ACCOUNT_CHANGE, SecurityUtils.getCurrentUserId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/account-change-archive")
    public ResponseEntity<Void> refreshAccountChangeArchive() {
        evictService.evict(ActivityCache.ACCOUNT_CHANGE_ARCHIVE, SecurityUtils.getCurrentUserId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/expense")
    public ResponseEntity<Void> refreshExpense() {
        evictService.evict(ActivityCache.EXPENSE, SecurityUtils.getCurrentUserId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/revenue")
    public ResponseEntity<Void> refreshRevenue() {
        evictService.evict(ActivityCache.REVENUE, SecurityUtils.getCurrentUserId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/limit")
    public ResponseEntity<Void> refreshLimit() {
        evictService.evict(ActivityCache.LIMIT, SecurityUtils.getCurrentUserId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/piggy-bank")
    public ResponseEntity<Void> refreshPiggyBank() {
        evictService.evict(ActivityCache.PIGGY_BANK, SecurityUtils.getCurrentUserId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/settings")
    public ResponseEntity<Void> refreshSettings() {
        evictService.evict(ActivityCache.SETTINGS, SecurityUtils.getCurrentUserId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/risk-operation")
    public ResponseEntity<Void> refreshRiskOperation() {
        evictService.evict(ActivityCache.RISK_OPERATION, SecurityUtils.getCurrentUserId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/shared-account/change-history")
    public ResponseEntity<Void> refreshSharedAccountChangeHistory() {
        evictService.evict(ActivityCache.SHARED_ACCOUNT_CHANGE_HISTORY, SecurityUtils.getCurrentUserId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/shared-account")
    public ResponseEntity<Void> refreshSharedAccountActivityLog() {
        evictService.evict(ActivityCache.SHARED_ACCOUNT_ACTIVITY_LOG, SecurityUtils.getCurrentUserId());
        return ResponseEntity.noContent().build();
    }
}