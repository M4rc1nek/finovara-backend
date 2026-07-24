package com.finovara.reportservice.sharedaccount.report.finances.calculate.chart.cashflow.service;

import com.finovara.contracts.transaction.report.dto.DailyCashDto;
import com.finovara.reportservice.feignclient.FinanceBackendSharedReportClient;
import com.finovara.reportservice.sharedaccount.report.finances.calculate.chart.builder.SharedCashFlowChartService;
import com.finovara.reportservice.sharedaccount.report.finances.calculate.chart.dto.SharedCashFlowDto;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SharedTotalCashFlowChartService {

    private final FinanceBackendSharedReportClient reportClient;
    private final SharedCashFlowChartService cashFlowChartService;

    @Cacheable(value = "report:sharedTotalCashFlowChart", key = "#ownerId + ':' + #memberId")
    public List<SharedCashFlowDto> getCashFlowChart(Long ownerId, Long memberId) {
        List<DailyCashDto> expenses = reportClient.expensesGroupedByDate(ownerId, memberId);
        List<DailyCashDto> revenues = reportClient.revenuesGroupedByDate(ownerId, memberId);
        return cashFlowChartService.getSharedCashFlowChart(expenses, revenues);
    }
}