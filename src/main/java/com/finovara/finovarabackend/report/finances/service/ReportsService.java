package com.finovara.finovarabackend.report.finances.service;

import com.finovara.finovarabackend.expense.repository.ExpenseRepository;
import com.finovara.finovarabackend.report.finances.dto.ReportMonthlyChartDTO;
import com.finovara.finovarabackend.revenue.repository.RevenueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReportsService {
    private final RevenueRepository revenueRepository;
    private final ExpenseRepository expenseRepository;

    public List<ReportMonthlyChartDTO> getMonthlyChart(Long userId) {
        LocalDate now = LocalDate.now();
        int daysInMonth = now.lengthOfMonth();
        List<ReportMonthlyChartDTO> chartData = new ArrayList<>();

        for (int day = 1; day <= daysInMonth; day++) {
            LocalDate targetDate = LocalDate.of(now.getYear(), now.getMonthValue(), day);

            BigDecimal dayIncome = revenueRepository.sumRevenueForDay(userId, targetDate);
            BigDecimal dayExpense = expenseRepository.sumExpenseForDay(userId, targetDate);

            chartData.add(new ReportMonthlyChartDTO(day, dayIncome, dayExpense));
        }
        return chartData;
    }

}
