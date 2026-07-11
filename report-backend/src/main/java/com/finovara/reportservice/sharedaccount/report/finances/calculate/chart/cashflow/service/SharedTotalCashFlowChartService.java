package com.finovara.reportservice.sharedaccount.report.finances.chart.cashflow.service;

import com.finovara.contracts.transaction.report.dto.DailyCashDto;
import com.finovara.reportservice.feignclient.FinanceBackendSharedReportClient;
import com.finovara.reportservice.sharedaccount.report.finances.chart.builder.SharedCashFlowChartService;
import com.finovara.reportservice.sharedaccount.report.finances.chart.dto.SharedCashFlowDto;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SharedTotalCashFlowChartService {

    private final FinanceBackendSharedReportClient reportClient;
    private final SharedCashFlowChartService cashFlowChartService;

    @Cacheable(value = "report:sharedTotalCashFlowChart", key = "#userId")
    public List<SharedCashFlowDto> getCashFlowChart(Long userId) {
        return getCashFlowChart(userId, null);
    }

    public List<SharedCashFlowDto> getCashFlowChart(Long userId, java.time.LocalDate today) {
        List<DailyCashDto> expenses = reportClient.expensesGroupedByDate(userId);
        List<DailyCashDto> revenues = reportClient.revenuesGroupedByDate(userId);
        return today == null
                ? cashFlowChartService.getSharedCashFlowChart(expenses, revenues)
                : cashFlowChartService.getSharedCashFlowChart(expenses, revenues, today);
    }
}