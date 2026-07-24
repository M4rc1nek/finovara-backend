package com.finovara.reportservice.report.finances.calculate.chart.cashflow.service;

import com.finovara.reportservice.feignclient.FinanceBackendReportClient;
import com.finovara.reportservice.report.finances.calculate.chart.builder.CashFlowChartService;
import com.finovara.reportservice.report.finances.calculate.chart.dto.CashFlowDto;
import com.finovara.contracts.transaction.report.dto.DailyCashDto;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TotalCashFlowChartService {

    private final FinanceBackendReportClient reportClient;
    private final CashFlowChartService cashFlowChartService;

    @Cacheable(value = "report:totalCashFlowChart", key = "#userId")
    public List<CashFlowDto> getCashFlowChart(Long userId) {
        return getCashFlowChart(userId, null);
    }

    public List<CashFlowDto> getCashFlowChart(Long userId, java.time.LocalDate today) {
        List<DailyCashDto> expenses = reportClient.expensesGroupedByDate(userId);
        List<DailyCashDto> revenues = reportClient.revenuesGroupedByDate(userId);
        return today == null
                ? cashFlowChartService.getCashFlowChart(expenses, revenues)
                : cashFlowChartService.getCashFlowChart(expenses, revenues, today);
    }
}