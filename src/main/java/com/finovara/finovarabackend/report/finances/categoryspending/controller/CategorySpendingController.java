package com.finovara.finovarabackend.report.finances.categoryspending.controller;

import com.finovara.finovarabackend.expense.model.ExpenseCategory;
import com.finovara.finovarabackend.report.finances.categoryspending.dto.CategorySpendingDto;
import com.finovara.finovarabackend.report.finances.categoryspending.service.CategorySpendingService;
import com.finovara.finovarabackend.security.SecurityUtils;
import com.finovara.finovarabackend.util.model.PeriodType;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports/category-spending")
@RequiredArgsConstructor
public class CategorySpendingController {
    private final CategorySpendingService categorySpendingService;

    @GetMapping
    public ResponseEntity<CategorySpendingDto> categorySpending(@RequestParam ExpenseCategory category, @RequestParam PeriodType periodType) {
        return ResponseEntity.ok(categorySpendingService.getCategorySpendingReport(SecurityUtils.getCurrentUserEmail(), category, periodType));
    }

}
