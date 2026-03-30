package com.finovara.finovarabackend.report.finances.chart.cashflow.service;

import com.finovara.finovarabackend.expense.repository.ExpenseRepository;
import com.finovara.finovarabackend.report.finances.chart.dto.CashFlowDto;
import com.finovara.finovarabackend.report.finances.chart.dto.DateAmountDto;
import com.finovara.finovarabackend.revenue.repository.RevenueRepository;
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
    private final RevenueRepository revenueRepository;
    private final ExpenseRepository expenseRepository;

    public List<CashFlowDto> getCashFlowChart(Long userId) {
        LocalDate today = LocalDate.now();
        LocalDate startOfMonth = today.withDayOfMonth(1);

        List<DateAmountDto> expenses = expenseRepository.sumExpensesGroupedByDate(userId);
        List<DateAmountDto> revenues = revenueRepository.sumRevenuesGroupedByDate(userId);

        Map<LocalDate, BigDecimal> expenseMap = expenses.stream()
                .collect(Collectors.toMap(DateAmountDto::date, DateAmountDto::amount));

        Map<LocalDate, BigDecimal> revenueMap = revenues.stream()
                .collect(Collectors.toMap(DateAmountDto::date, DateAmountDto::amount));

        List<CashFlowDto> result = new ArrayList<>();

        for (LocalDate date = startOfMonth; !date.isAfter(today); date = date.plusDays(1)) {
            result.add(new CashFlowDto(date, revenueMap.getOrDefault(date, BigDecimal.ZERO),
                    expenseMap.getOrDefault(date, BigDecimal.ZERO)));
        }

        return result;
    }
}
