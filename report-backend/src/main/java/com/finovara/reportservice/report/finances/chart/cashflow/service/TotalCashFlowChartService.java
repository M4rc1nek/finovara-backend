package com.finovara.corebackend.report.finances.chart.cashflow.service;

import com.finovara.corebackend.expense.repository.ExpenseRepository;
import com.finovara.corebackend.report.finances.chart.builder.CashFlowChartService;
import com.finovara.corebackend.report.finances.chart.dto.CashFlowDto;
import com.finovara.corebackend.report.finances.chart.dto.DailyCashDto;
import com.finovara.corebackend.revenue.repository.RevenueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TotalCashFlowChartService {
    private final RevenueRepository revenueRepository;
    private final ExpenseRepository expenseRepository;
    private final CashFlowChartService cashFlowChartService;

    public List<CashFlowDto> getCashFlowChart(Long userId) {
        List<DailyCashDto> summedExpenses = expenseRepository.sumExpensesGroupedByDate(userId);
        List<DailyCashDto> summedRevenues = revenueRepository.sumRevenuesGroupedByDate(userId);
        return cashFlowChartService.getCashFlowChart(summedExpenses, summedRevenues);
    }
}