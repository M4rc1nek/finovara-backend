package com.finovara.corebackend.report.finances.chart.averagecashflow.service;

import com.finovara.corebackend.expense.repository.ExpenseRepository;
import com.finovara.corebackend.report.finances.chart.dto.CashFlowDto;
import com.finovara.corebackend.report.finances.chart.dto.DailyCashDto;
import com.finovara.corebackend.report.finances.chart.builder.CashFlowChartService;
import com.finovara.corebackend.revenue.repository.RevenueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AverageCashFlowChartService {
    private final RevenueRepository revenueRepository;
    private final ExpenseRepository expenseRepository;
    private final CashFlowChartService cashFlowChartService;

    public List<CashFlowDto> getAverageCashFlowChart(Long userId) {
        List<DailyCashDto> averageExpenses = expenseRepository.avgExpensesGroupedByDate(userId);
        List<DailyCashDto> averageRevenues = revenueRepository.avgRevenuesGroupedByDate(userId);
        return  cashFlowChartService.getCashFlowChart(averageExpenses, averageRevenues);
    }

}
