package com.finovara.finovarabackend.report.finances.categoryearned.controller;

import com.finovara.finovarabackend.report.finances.categoryearned.dto.CategoryEarnedDto;
import com.finovara.finovarabackend.report.finances.categoryearned.service.RevenuePercentageByCategory;
import com.finovara.finovarabackend.revenue.model.RevenueCategory;
import com.finovara.finovarabackend.security.SecurityUtils;
import com.finovara.finovarabackend.util.model.PeriodType;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports/category-earned")
@RequiredArgsConstructor
public class CategoryEarnedController {
    private final RevenuePercentageByCategory revenuePercentageByCategory;

    @GetMapping
    public ResponseEntity<CategoryEarnedDto> categorySpending(@RequestParam RevenueCategory category, @RequestParam PeriodType periodType) {
        return ResponseEntity.ok(revenuePercentageByCategory.getRevenuePercentageByCategoryReport(SecurityUtils.getCurrentUserEmail(), category, periodType));
    }
}
