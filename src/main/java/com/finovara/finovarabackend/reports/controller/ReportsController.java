package com.finovara.finovarabackend.reports.controller;

import com.finovara.finovarabackend.reports.dto.ReportMonthlyChartDTO;
import com.finovara.finovarabackend.reports.dto.ReportsAverageDTO;
import com.finovara.finovarabackend.reports.dto.ReportsHighestExpense;
import com.finovara.finovarabackend.reports.dto.ReportsSumDTO;
import com.finovara.finovarabackend.reports.service.ReportsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportsController {
    private final ReportsService reportsService;

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
}
