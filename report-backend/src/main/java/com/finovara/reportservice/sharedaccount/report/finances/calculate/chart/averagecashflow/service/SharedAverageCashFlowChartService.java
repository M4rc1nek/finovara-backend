package com.finovara.reportservice.sharedaccount.report.finances.calculate.chart.averagecashflow.service;

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
public class SharedAverageCashFlowChartService {

    private final FinanceBackendSharedReportClient reportClient;
    private final SharedCashFlowChartService cashFlowChartService;

    @Cacheable(value = "report:sharedAverageCashFlowChart", key = "#ownerId + ':' + #memberId")
    public List<SharedCashFlowDto> getAverageCashFlowChart(Long ownerId, Long memberId) {
        List<DailyCashDto> expenses = reportClient.expensesAvgGroupedByDate(ownerId, memberId);
        List<DailyCashDto> revenues = reportClient.revenuesAvgGroupedByDate(ownerId, memberId);
        return cashFlowChartService.getSharedCashFlowChart(expenses, revenues);
    }
}