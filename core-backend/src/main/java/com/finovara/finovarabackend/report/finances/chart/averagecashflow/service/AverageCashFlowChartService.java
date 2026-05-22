package com.finovara.finovarabackend.report.finances.chart.averagecashflow.service;

import com.finovara.finovarabackend.expense.repository.ExpenseRepository;
import com.finovara.finovarabackend.report.finances.chart.dto.CashFlowDto;
import com.finovara.finovarabackend.report.finances.chart.dto.DailyCashDto;
import com.finovara.finovarabackend.report.finances.chart.builder.CashFlowChartService;
import com.finovara.finovarabackend.revenue.repository.RevenueRepository;
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
