package com.finovara.finovarabackend.report.finances.sum.controller;

import com.finovara.finovarabackend.report.dto.ReportDto;
import com.finovara.finovarabackend.util.model.PeriodType;
import com.finovara.finovarabackend.report.finances.sum.sevice.ReportSummaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/reports/sum")
@RequiredArgsConstructor
public class ReportSumController {
    private final ReportSummaryService reportSummaryService;

    @GetMapping("/{userId}")
    public ResponseEntity<ReportDto> getSummedExpense(@PathVariable Long userId, @RequestParam PeriodType periodType) {
        return ResponseEntity.ok(reportSummaryService.sumExpense(userId, periodType));
    }

    @GetMapping("/{userId}/revenue")
    public ResponseEntity<ReportDto> getSummedRevenue(@PathVariable Long userId, @RequestParam PeriodType periodType) {
        return ResponseEntity.ok(reportSummaryService.sumRevenue(userId, periodType));
    }
}
