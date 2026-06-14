package com.finovara.reportservice.report.finances.chart.averagecashflow.service;

import com.finovara.contracts.transaction.report.dto.DailyCashDto;
import com.finovara.reportservice.feignclient.FinanceBackendReportClient;
import com.finovara.reportservice.report.finances.chart.builder.CashFlowChartService;
import com.finovara.reportservice.report.finances.chart.dto.CashFlowDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AverageCashFlowChartService {

    private final FinanceBackendReportClient reportClient;
    private final CashFlowChartService cashFlowChartService;

    public List<CashFlowDto> getAverageCashFlowChart(Long userId) {
        List<DailyCashDto> expenses = reportClient.expensesAvgGroupedByDate(userId);
        List<DailyCashDto> revenues = reportClient.revenuesAvgGroupedByDate(userId);
        return cashFlowChartService.getCashFlowChart(expenses, revenues);
    }
}