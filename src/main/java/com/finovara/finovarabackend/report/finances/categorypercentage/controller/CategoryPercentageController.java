package com.finovara.finovarabackend.report.finances.categorypercentage.controller;

import com.finovara.finovarabackend.expense.model.ExpenseCategory;
import com.finovara.finovarabackend.report.finances.categorypercentage.categoryearned.dto.CategoryEarnedDto;
import com.finovara.finovarabackend.report.finances.categorypercentage.categoryearned.service.RevenueCategoryService;
import com.finovara.finovarabackend.report.finances.categorypercentage.categoryspending.dto.CategorySpendingDto;
import com.finovara.finovarabackend.report.finances.categorypercentage.categoryspending.service.ExpenseCategoryService;
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
@RequestMapping("/api/reports/category-percentage")
@RequiredArgsConstructor
public class CategoryPercentageController {
    private final RevenueCategoryService revenueCategoryService;
    private final ExpenseCategoryService expenseCategoryService;

    @GetMapping("/expense")
    public ResponseEntity<CategorySpendingDto> categorySpending(@RequestParam ExpenseCategory category, @RequestParam PeriodType periodType) {
        return ResponseEntity.ok(expenseCategoryService.getExpensePercentageByCategoryReport(SecurityUtils.getCurrentUserEmail(), category, periodType));
    }

    @GetMapping("/revenue")
    public ResponseEntity<CategoryEarnedDto> categoryEarned(@RequestParam RevenueCategory category, @RequestParam PeriodType periodType) {
        return ResponseEntity.ok(revenueCategoryService.getRevenuePercentageByCategoryReport(SecurityUtils.getCurrentUserEmail(), category, periodType));
    }

}
