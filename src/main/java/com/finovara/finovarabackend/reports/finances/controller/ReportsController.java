package com.finovara.finovarabackend.reports.finances.controller;

import com.finovara.finovarabackend.expense.model.ExpenseCategory;
import com.finovara.finovarabackend.reports.finances.dto.*;
import com.finovara.finovarabackend.reports.finances.service.ReportsCategorySpendingService;
import com.finovara.finovarabackend.reports.finances.service.ReportsService;
import com.finovara.finovarabackend.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportsController {
    private final ReportsService reportsService;
    private final ReportsCategorySpendingService reportsCategorySpendingService;

    @GetMapping("/sum/{userId}")
    public ResponseEntity<ReportsSumDTO> sumRevenueAndExpense(@PathVariable Long userId) {
        return ResponseEntity.ok(reportsService.getTotalRevenueAndExpense(userId));
    }

    @GetMapping("/average/{userId}")
    public ResponseEntity<ReportsAverageDTO> averageRevenueAndExpense(@PathVariable Long userId) {
        return ResponseEntity.ok(reportsService.getAverageRevenueAndExpense(userId));
    }

    @GetMapping("/highest/{userId}")
    public ResponseEntity<List<ReportsHighestExpense>> highestExpense(@PathVariable Long userId) {
        return ResponseEntity.ok(reportsService.getHighestExpense(userId));
    }

    @GetMapping("/chart/{userId}")
    public List<ReportMonthlyChartDTO> chart(@PathVariable Long userId) {
        return reportsService.getMonthlyChart(userId);
    }

    @GetMapping
    public ResponseEntity<CategorySpendingDto> categorySpending(ExpenseCategory category) {
        return ResponseEntity.ok(reportsCategorySpendingService.getCategorySpendingReport(SecurityUtils.getCurrentUserEmail(), category));
    }

}
