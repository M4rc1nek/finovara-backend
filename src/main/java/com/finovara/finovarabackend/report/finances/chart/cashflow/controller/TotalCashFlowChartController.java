package com.finovara.finovarabackend.report.finances.chart.cashflow.controller;

import com.finovara.finovarabackend.report.finances.chart.cashflow.dto.TotalCashFlowDto;
import com.finovara.finovarabackend.report.finances.chart.cashflow.service.TotalCashFlowChartService;
import com.finovara.finovarabackend.util.model.PeriodType;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reports/cash-flow-chart")
@RequiredArgsConstructor
public class TotalCashFlowChartController {
    private final TotalCashFlowChartService totalCashFlowChartService;

    @GetMapping("/{userId}")
    public ResponseEntity<List<TotalCashFlowDto>> getCashFlowChart(@PathVariable Long userId, @RequestParam PeriodType periodType) {
        return ResponseEntity.ok(totalCashFlowChartService.getCashFlowChart(userId, periodType));
    }
}
