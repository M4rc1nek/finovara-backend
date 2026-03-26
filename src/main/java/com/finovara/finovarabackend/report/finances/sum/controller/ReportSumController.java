package com.finovara.finovarabackend.report.finances.sum.controller;

import com.finovara.finovarabackend.report.dto.ReportDto;
import com.finovara.finovarabackend.report.model.ReportPeriodType;
import com.finovara.finovarabackend.report.finances.sum.sevice.ReportSumService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/reports/sum")
@RequiredArgsConstructor
public class ReportSumController {
    private final ReportSumService reportSumService;

    @GetMapping("/{userId}")
    public ResponseEntity<ReportDto> getSummedExpense(@PathVariable Long userId, @RequestParam ReportPeriodType reportPeriodType) {
        return ResponseEntity.ok(reportSumService.sumExpense(userId, reportPeriodType));
    }

    @GetMapping("/{userId}/revenue")
    public ResponseEntity<ReportDto> getSummedRevenue(@PathVariable Long userId, @RequestParam ReportPeriodType reportPeriodType) {
        return ResponseEntity.ok(reportSumService.sumRevenue(userId, reportPeriodType));
    }
}
