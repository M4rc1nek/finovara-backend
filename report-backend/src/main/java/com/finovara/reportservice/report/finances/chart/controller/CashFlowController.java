package com.finovara.corebackend.report.finances.chart.controller;

import com.finovara.corebackend.report.finances.chart.averagecashflow.service.AverageCashFlowChartService;
import com.finovara.corebackend.report.finances.chart.cashflow.service.TotalCashFlowChartService;
import com.finovara.corebackend.report.finances.chart.dto.CashFlowDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/reports/cash-flow-chart")
@RequiredArgsConstructor
public class CashFlowController {
    private final TotalCashFlowChartService totalCashFlowChartService;
    private final AverageCashFlowChartService averageCashFlowChartService;

    @GetMapping("/sum/{userId}")
    public ResponseEntity<List<CashFlowDto>> getCashFlowChart(@PathVariable Long userId) {
        return ResponseEntity.ok(totalCashFlowChartService.getCashFlowChart(userId));
    }

    @GetMapping("/average/{userId}")
    public ResponseEntity<List<CashFlowDto>> getAverageCashFlowChart(@PathVariable Long userId) {
        return ResponseEntity.ok(averageCashFlowChartService.getAverageCashFlowChart(userId));
    }

}
