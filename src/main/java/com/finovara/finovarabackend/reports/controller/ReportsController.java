package com.finovara.finovarabackend.reports.controller;

import com.finovara.finovarabackend.expense.model.ExpenseCategory;
import com.finovara.finovarabackend.reports.dto.*;
import com.finovara.finovarabackend.reports.service.ReportsCategorySpendingService;
import com.finovara.finovarabackend.reports.service.ReportsService;
import com.finovara.finovarabackend.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportsController {
    private final ReportsService reportsService;
    private final ReportsCategorySpendingService reportsCategorySpendingService;

    @GetMapping("/sum/{userId}")
    public ResponseEntity<ReportsSumDTO> sumRevenueAndExpense(@PathVariable Long userId) {
        return ResponseEntity.ok(reportsService.sumRevenueAndExpense(userId));
    }

    @GetMapping("/average/{userId}")
    public ResponseEntity<ReportsAverageDTO> avgRevenueAndExpense(@PathVariable Long userId) {
        return ResponseEntity.ok(reportsService.averageRevenueAndExpense(userId));
    }

    @GetMapping("/highest/{userId}")
    public ResponseEntity<List<ReportsHighestExpense>> highestExpense(@PathVariable Long userId) {
        return ResponseEntity.ok(reportsService.highestExpense(userId));
    }

    @GetMapping("/chart/{userId}")
    public List<ReportMonthlyChartDTO> getChart(@PathVariable Long userId) {
        return reportsService.getMonthlyChart(userId);
    }

    @GetMapping
    public ResponseEntity<CategorySpendingDto> getCategorySpending(ExpenseCategory category) {
        return ResponseEntity.ok(reportsCategorySpendingService.getCategorySpendingReport(SecurityUtils.getCurrentUserEmail(), category));
    }

}
