package com.finovara.finovarabackend.report.finances.categoryspending.controller;

import com.finovara.finovarabackend.expense.model.ExpenseCategory;
import com.finovara.finovarabackend.report.finances.dto.CategorySpendingDto;
import com.finovara.finovarabackend.report.finances.categoryspending.service.ReportsCategorySpendingService;
import com.finovara.finovarabackend.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportsCategorySpendingController {
    private final ReportsCategorySpendingService reportsCategorySpendingService;

    @GetMapping
    public ResponseEntity<CategorySpendingDto> categorySpending(ExpenseCategory category) {
        return ResponseEntity.ok(reportsCategorySpendingService.getCategorySpendingReport(SecurityUtils.getCurrentUserEmail(), category));
    }

}
