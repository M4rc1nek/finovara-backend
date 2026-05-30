package com.finovara.corebackend.report.finances.categorypercentage.controller;

import com.finovara.contracts.model.transaction.ExpenseCategory;
import com.finovara.corebackend.report.finances.categorypercentage.revenue.dto.RevenueCategoryPercentageDto;
import com.finovara.corebackend.report.finances.categorypercentage.revenue.service.RevenueCategoryPercentageService;
import com.finovara.corebackend.report.finances.categorypercentage.expense.dto.ExpenseCategoryPercentageDto;
import com.finovara.corebackend.report.finances.categorypercentage.expense.service.ExpenseCategoryPercentageService;
import com.finovara.contracts.model.transaction.RevenueCategory;
import com.finovara.corebackend.security.SecurityUtils;
import com.finovara.contracts.model.PeriodType;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports/category-percentage")
@RequiredArgsConstructor
public class CategoryPercentageController {
    private final RevenueCategoryPercentageService revenueCategoryPercentageService;
    private final ExpenseCategoryPercentageService expenseCategoryPercentageService;

    @GetMapping("/expense")
    public ResponseEntity<ExpenseCategoryPercentageDto> getExpensePercentageByCategory(@RequestParam ExpenseCategory category, @RequestParam PeriodType periodType) {
        return ResponseEntity.ok(expenseCategoryPercentageService.getExpensePercentageByCategoryReport(SecurityUtils.getCurrentUserId(), category, periodType));
    }

    @GetMapping("/revenue")
    public ResponseEntity<RevenueCategoryPercentageDto> getRevenuePercentageByCategory(@RequestParam RevenueCategory category, @RequestParam PeriodType periodType) {
        return ResponseEntity.ok(revenueCategoryPercentageService.getRevenuePercentageByCategoryReport(SecurityUtils.getCurrentUserId(), category, periodType));
    }

}
