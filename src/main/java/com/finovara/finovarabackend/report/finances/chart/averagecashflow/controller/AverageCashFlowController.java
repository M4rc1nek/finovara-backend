package com.finovara.finovarabackend.report.finances.chart.averagecashflow.controller;

import com.finovara.finovarabackend.report.finances.chart.averagecashflow.service.AverageCashFlowService;
import com.finovara.finovarabackend.report.finances.chart.dto.CashFlowDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/reports/average-cash-flow-chart")
@RequiredArgsConstructor
public class AverageCashFlowController {
    private final AverageCashFlowService averageCashFlowService;

    @GetMapping("/{userId}")
    public ResponseEntity<List<CashFlowDto>> getAverageCashFlowChart(@PathVariable Long userId) {
        return ResponseEntity.ok(averageCashFlowService.getAverageCashFlowChart(userId));
    }
}
