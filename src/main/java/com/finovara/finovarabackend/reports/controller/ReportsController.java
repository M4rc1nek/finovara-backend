package com.finovara.finovarabackend.reports.controller;

import com.finovara.finovarabackend.reports.dto.ReportsAverageDTO;
import com.finovara.finovarabackend.reports.dto.ReportsHighestExpense;
import com.finovara.finovarabackend.reports.dto.ReportsSumDTO;
import com.finovara.finovarabackend.reports.service.ReportsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ReportsController {
    private final ReportsService reportsService;

    @GetMapping("/sumRevenueAndExpense/{userId}")
    public ResponseEntity<ReportsSumDTO> sumRevenueAndExpense(@PathVariable Long userId) {
        return ResponseEntity.ok(reportsService.sumRevenueAndExpense(userId));
    }

    @GetMapping("/avgRevenueAndExpense/{userId}")
    public ResponseEntity<ReportsAverageDTO> avgRevenueAndExpense(@PathVariable Long userId) {
        return ResponseEntity.ok(reportsService.averageRevenueAndExpense(userId));
    }

    @GetMapping("/highestExpense/{userId}")
    public ResponseEntity<List<ReportsHighestExpense>> highestExpense(@PathVariable Long userId) {
        return ResponseEntity.ok(reportsService.highestExpense(userId));
    }
}
