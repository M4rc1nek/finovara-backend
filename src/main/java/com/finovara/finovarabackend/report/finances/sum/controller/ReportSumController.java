package com.finovara.finovarabackend.report.finances.sum.controller;

import com.finovara.finovarabackend.report.finances.sum.dto.ReportSumDto;
import com.finovara.finovarabackend.report.finances.sum.model.ReportSumType;
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
    public ResponseEntity<ReportSumDto> getSummedExpense(@PathVariable Long userId, @RequestParam ReportSumType reportSumType) {
        return ResponseEntity.ok(reportSumService.sumExpense(userId, reportSumType));
    }

    @GetMapping("/{userId}/revenue")
    public ResponseEntity<ReportSumDto> getSummedRevenue(@PathVariable Long userId, @RequestParam ReportSumType reportSumType) {
        return ResponseEntity.ok(reportSumService.sumRevenue(userId, reportSumType));
    }
}
