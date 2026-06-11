package com.finovara.reportservice.report.finances.chart.cashflow.service;

import com.finovara.reportservice.feignclient.CoreBackendReportClient;
import com.finovara.reportservice.report.finances.chart.builder.CashFlowChartService;
import com.finovara.reportservice.report.finances.chart.dto.CashFlowDto;
import com.finovara.contracts.transaction.report.dto.DailyCashDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TotalCashFlowChartService {

    private final CoreBackendReportClient reportClient;
    private final CashFlowChartService cashFlowChartService;

    public List<CashFlowDto> getCashFlowChart(Long userId) {
        List<DailyCashDto> expenses = reportClient.expensesGroupedByDate(userId);
        List<DailyCashDto> revenues = reportClient.revenuesGroupedByDate(userId);
        return cashFlowChartService.getCashFlowChart(expenses, revenues);
    }
}