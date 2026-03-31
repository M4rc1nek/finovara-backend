package com.finovara.finovarabackend.report.finances.chart.cashflow.service;

import com.finovara.finovarabackend.expense.repository.ExpenseRepository;
import com.finovara.finovarabackend.report.finances.chart.dto.CashFlowDto;
import com.finovara.finovarabackend.report.finances.chart.dto.DailyCashDto;
import com.finovara.finovarabackend.report.finances.chart.service.CashFlowChartBuilder;
import com.finovara.finovarabackend.revenue.repository.RevenueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TotalCashFlowChartService {
    private final RevenueRepository revenueRepository;
    private final ExpenseRepository expenseRepository;
    private final CashFlowChartBuilder cashFlowChartBuilder;

    public List<CashFlowDto> getCashFlowChart(Long userId) {
        List<DailyCashDto> summedExpenses = expenseRepository.sumExpensesGroupedByDate(userId);
        List<DailyCashDto> summedRevenues = revenueRepository.sumRevenuesGroupedByDate(userId);
        return cashFlowChartBuilder.getCashFlowTemplateChart(summedExpenses, summedRevenues);
    }
}