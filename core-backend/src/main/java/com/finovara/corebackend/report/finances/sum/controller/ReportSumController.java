package com.finovara.corebackend.report.finances.sum.controller;

import com.finovara.corebackend.report.dto.ReportDto;
import com.finovara.contracts.model.PeriodType;
import com.finovara.corebackend.report.finances.sum.service.ReportSummaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/reports/sum")
@RequiredArgsConstructor
public class ReportSumController {
    private final ReportSummaryService reportSummaryService;

    @GetMapping("/expense/{userId}")
    public ResponseEntity<ReportDto> getSummedExpense(@PathVariable Long userId, @RequestParam PeriodType periodType) {
        return ResponseEntity.ok(reportSummaryService.sumExpense(userId, periodType));
    }

    @GetMapping("/revenue/{userId}")
    public ResponseEntity<ReportDto> getSummedRevenue(@PathVariable Long userId, @RequestParam PeriodType periodType) {
        return ResponseEntity.ok(reportSummaryService.sumRevenue(userId, periodType));
    }
}
