package com.finovara.financeservice.history.expensehistory.cache.evict.controller;

import com.finovara.financeservice.history.expensehistory.cache.evict.service.EvictExpenseCacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/expense-history-cache")
@RequiredArgsConstructor
public class RefreshExpenseCacheController {

    private final EvictExpenseCacheService evictExpenseCacheService;

    @PostMapping("/refresh")
    public ResponseEntity<Void> refreshExpenseHistoryData(@RequestHeader("X-User-Id") Long userId) {
        evictExpenseCacheService.evictDataForUser(userId);
        return ResponseEntity.ok().build();
    }
}
