package com.finovara.finovarabackend.revenuehistory.controller;

import com.finovara.finovarabackend.revenue.dto.RevenueDto;
import com.finovara.finovarabackend.revenue.model.RevenueCategory;
import com.finovara.finovarabackend.revenuehistory.service.RevenueHistoryService;
import com.finovara.finovarabackend.security.SecurityUtils;
import com.finovara.finovarabackend.util.model.PeriodType;
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
