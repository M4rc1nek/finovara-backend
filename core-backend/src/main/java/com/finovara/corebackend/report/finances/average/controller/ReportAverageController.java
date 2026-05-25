package com.finovara.corebackend.report.finances.average.controller;

import com.finovara.corebackend.report.dto.ReportDto;
import com.finovara.corebackend.report.finances.average.service.ReportAverageService;
import com.finovara.activityservice.contracts.model.PeriodType;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/reports/average")
@RequiredArgsConstructor
public class ReportAverageController {
    private final ReportAverageService reportAverageService;

    @GetMapping("/expense/{userId}")
    public ResponseEntity<ReportDto> getAverageExpense(@PathVariable Long userId, @RequestParam PeriodType periodType) {
        return ResponseEntity.ok(reportAverageService.calculateAverageExpense(userId, periodType));
    }

    @GetMapping("/revenue/{userId}")
    public ResponseEntity<ReportDto> getAverageRevenue(@PathVariable Long userId, @RequestParam PeriodType periodType) {
        return ResponseEntity.ok(reportAverageService.calculateAverageRevenue(userId, periodType));
    }
}
