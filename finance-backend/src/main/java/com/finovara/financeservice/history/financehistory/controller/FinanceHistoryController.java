package com.finovara.financeservice.history.financehistory.controller;

import com.finovara.contracts.util.PeriodType;
import com.finovara.contracts.util.model.ExpenseCategory;
import com.finovara.contracts.util.model.RevenueCategory;
import com.finovara.financeservice.expense.dto.ExpenseDto;
import com.finovara.financeservice.history.cache.evict.EvictFinanceDataHistoryService;
import com.finovara.financeservice.history.financehistory.service.FinanceHistoryService;
import com.finovara.financeservice.revenue.dto.RevenueDto;
import com.finovara.financeservice.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/finance-history")
@RequiredArgsConstructor
public class FinanceHistoryController {

    private final FinanceHistoryService financeHistoryService;
    private final EvictFinanceDataHistoryService evictFinanceDataHistoryService;

    @GetMapping("/revenue")
    public ResponseEntity<List<RevenueDto>> getRevenueHistory(@RequestParam PeriodType periodType, @RequestParam RevenueCategory category) {
        return ResponseEntity.ok(financeHistoryService.getRevenueByCategory(SecurityUtils.getCurrentUserId(), periodType, category));
    }

    @GetMapping("/expense")
    public ResponseEntity<List<ExpenseDto>> getExpenseHistory(@RequestParam PeriodType periodType, @RequestParam ExpenseCategory category) {
        return ResponseEntity.ok(financeHistoryService.getExpenseByCategory(SecurityUtils.getCurrentUserId(), periodType, category));
    }

    @PostMapping("/revenue/refresh")
    public ResponseEntity<Void> refreshRevenueHistoryData() {
        evictFinanceDataHistoryService.evictRevenueHistoryForUser(SecurityUtils.getCurrentUserId());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/expense/refresh")
    public ResponseEntity<Void> refreshExpenseHistoryData() {
        evictFinanceDataHistoryService.evictExpenseHistoryForUser(SecurityUtils.getCurrentUserId());
        return ResponseEntity.ok().build();
    }

}

