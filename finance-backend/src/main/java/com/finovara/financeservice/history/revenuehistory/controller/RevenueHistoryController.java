package com.finovara.financeservice.history.revenuehistory.controller;

import com.finovara.financeservice.revenue.dto.RevenueDto;
import com.finovara.contracts.util.model.RevenueCategory;
import com.finovara.financeservice.history.revenuehistory.service.RevenueHistoryService;
import com.finovara.contracts.util.PeriodType;
import com.finovara.financeservice.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/revenue-history")
@RequiredArgsConstructor
public class RevenueHistoryController {

    private final RevenueHistoryService revenueHistoryService;

    @GetMapping
    public ResponseEntity<List<RevenueDto>> getRevenueHistory(@RequestParam PeriodType periodType, @RequestParam RevenueCategory category) {
        return ResponseEntity.ok(revenueHistoryService.getRevenueByCategory(SecurityUtils.getCurrentUserId(), periodType, category));
    }
}
