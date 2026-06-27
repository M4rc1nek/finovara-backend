package com.finovara.financeservice.history.expensehistory.cache.refresh.controller;

import com.finovara.financeservice.history.expensehistory.cache.refresh.service.RefreshCacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/expense-history-cache")
@RequiredArgsConstructor
public class RefreshCacheController {

    private final RefreshCacheService refreshCacheService;

    @PostMapping("/refresh")
    public ResponseEntity<Void> refreshExpenseHistoryData(@RequestHeader("X-User-Id") Long userId) {
        refreshCacheService.evictDataForUser(userId);
        return ResponseEntity.ok().build();
    }
}
