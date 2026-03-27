package com.finovara.finovarabackend.report.finances.average.controller;

import com.finovara.finovarabackend.report.dto.ReportDto;
import com.finovara.finovarabackend.report.finances.average.service.ReportAverageService;
import com.finovara.finovarabackend.util.model.PeriodType;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/reports/average")
@RequiredArgsConstructor
public class ReportAverageController {
    private final ReportAverageService reportAverageService;

    @GetMapping("/{userId}")
    public ResponseEntity<ReportDto> getAverageExpense(@PathVariable Long userId, @RequestParam PeriodType periodType) {
        return ResponseEntity.ok(reportAverageService.calculateAverageExpense(userId, periodType));
    }

    @GetMapping("/{userId}/revenue")
    public ResponseEntity<ReportDto> getAverageRevenue(@PathVariable Long userId, @RequestParam PeriodType periodType) {
        return ResponseEntity.ok(reportAverageService.calculateAverageRevenue(userId, periodType));
    }
}
