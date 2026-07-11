package com.finovara.reportservice.report.finances.chart.averagecashflow.service;

import com.finovara.contracts.transaction.report.dto.DailyCashDto;
import com.finovara.reportservice.feignclient.FinanceBackendReportClient;
import com.finovara.reportservice.report.finances.chart.builder.CashFlowChartService;
import com.finovara.reportservice.report.finances.chart.dto.CashFlowDto;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AverageCashFlowChartService {

    private final FinanceBackendReportClient reportClient;
    private final CashFlowChartService cashFlowChartService;

    @Cacheable(value = "report:averageCashFlowChart", key = "#userId")
    public List<CashFlowDto> getAverageCashFlowChart(Long userId) {
        return getAverageCashFlowChart(userId, null);
    }

    public List<CashFlowDto> getAverageCashFlowChart(Long userId, java.time.LocalDate today) {
        List<DailyCashDto> expenses = reportClient.expensesAvgGroupedByDate(userId);
        List<DailyCashDto> revenues = reportClient.revenuesAvgGroupedByDate(userId);
        return today == null
                ? cashFlowChartService.getCashFlowChart(expenses, revenues)
                : cashFlowChartService.getCashFlowChart(expenses, revenues, today);
    }
}