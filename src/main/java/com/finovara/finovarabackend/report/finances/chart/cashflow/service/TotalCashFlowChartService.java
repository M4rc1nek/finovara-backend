package com.finovara.finovarabackend.report.finances.chart.cashflow.service;

import com.finovara.finovarabackend.expense.repository.ExpenseRepository;
import com.finovara.finovarabackend.report.finances.chart.cashflow.dto.DailyAmountDto;
import com.finovara.finovarabackend.report.finances.chart.cashflow.dto.TotalCashFlowDto;
import com.finovara.finovarabackend.revenue.repository.RevenueRepository;
import com.finovara.finovarabackend.util.model.PeriodType;
import com.finovara.finovarabackend.util.service.periodbalance.FinancialPeriodService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TotalCashFlowChartService {
    private final FinancialPeriodService financialPeriodService;
    private final RevenueRepository revenueRepository;
    private final ExpenseRepository expenseRepository;

    public List<TotalCashFlowDto> getCashFlowChart(Long userId, PeriodType periodType) {
        LocalDate today = LocalDate.now();
        LocalDate start = financialPeriodService.getStartDate(today, periodType);

        List<DailyAmountDto> expenses = expenseRepository.sumExpensesGroupedByDate(userId, start, today);
        List<DailyAmountDto> revenues = revenueRepository.sumRevenuesGroupedByDate(userId, start, today);

        Map<LocalDate, BigDecimal> expenseMap = expenses.stream()
                .collect(Collectors.toMap(DailyAmountDto::date, DailyAmountDto::amount));

        Map<LocalDate, BigDecimal> revenueMap = revenues.stream()
                .collect(Collectors.toMap(DailyAmountDto::date, DailyAmountDto::amount));

        List<TotalCashFlowDto> result = new ArrayList<>();

        for (LocalDate date = start; !date.isAfter(today); date = date.plusDays(1)) {
            result.add(new TotalCashFlowDto(date, revenueMap.getOrDefault(date, BigDecimal.ZERO),
                    expenseMap.getOrDefault(date, BigDecimal.ZERO)));
        }

        return result;
    }
}
