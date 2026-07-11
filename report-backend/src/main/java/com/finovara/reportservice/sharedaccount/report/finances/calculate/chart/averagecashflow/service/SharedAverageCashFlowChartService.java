package com.finovara.reportservice.sharedaccount.report.finances.chart.averagecashflow.service;

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
public class SharedAverageCashFlowChartService {

    private final FinanceBackendSharedReportClient reportClient;
    private final SharedCashFlowChartService cashFlowChartService;

    @Cacheable(value = "report:sharedAverageCashFlowChart", key = "#userId")
    public List<SharedCashFlowDto> getAverageCashFlowChart(Long userId) {
        return getAverageCashFlowChart(userId, null);
    }

    public List<SharedCashFlowDto> getAverageCashFlowChart(Long userId, java.time.LocalDate today) {
        List<DailyCashDto> expenses = reportClient.expensesAvgGroupedByDate(userId);
        List<DailyCashDto> revenues = reportClient.revenuesAvgGroupedByDate(userId);
        return today == null
                ? cashFlowChartService.getSharedCashFlowChart(expenses, revenues)
                : cashFlowChartService.getSharedCashFlowChart(expenses, revenues, today);
    }
}