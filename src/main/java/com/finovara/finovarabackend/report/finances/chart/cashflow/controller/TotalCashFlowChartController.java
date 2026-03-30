package com.finovara.finovarabackend.report.finances.chart.cashflow.controller;

import com.finovara.finovarabackend.report.finances.chart.cashflow.service.TotalCashFlowChartService;
import com.finovara.finovarabackend.report.finances.chart.dto.CashFlowDto;
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
public class TotalCashFlowChartController {
    private final TotalCashFlowChartService totalCashFlowChartService;

    @GetMapping("/{userId}")
    public ResponseEntity<List<CashFlowDto>> getCashFlowChart(@PathVariable Long userId) {
        return ResponseEntity.ok(totalCashFlowChartService.getCashFlowChart(userId));
    }
}
