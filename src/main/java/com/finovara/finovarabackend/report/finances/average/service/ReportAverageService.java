package com.finovara.finovarabackend.report.finances.average.service;

import com.finovara.finovarabackend.expense.model.Expense;
import com.finovara.finovarabackend.expense.repository.ExpenseRepository;
import com.finovara.finovarabackend.report.dto.ReportDto;
import com.finovara.finovarabackend.revenue.model.Revenue;
import com.finovara.finovarabackend.revenue.repository.RevenueRepository;
import com.finovara.finovarabackend.util.model.PeriodType;
import com.finovara.finovarabackend.util.service.periodbalance.FinancialPeriodService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReportAverageService {

    private final FinancialPeriodService financialPeriodService;
    private final ExpenseRepository expenseRepository;
    private final RevenueRepository revenueRepository;

    public ReportDto calculateAverageExpense(Long userId, PeriodType periodType) {
        List<Expense> expenses = expenseRepository.findAllByUserAssignedId(userId);
        BigDecimal amount = calculateAverage(financialPeriodService.getSpent(userId, periodType), expenses);

        return new ReportDto(periodType, amount);
    }

    public ReportDto calculateAverageRevenue(Long userId, PeriodType periodType) {
        List<Revenue> revenues = revenueRepository.findAllByUserAssignedId(userId);
        BigDecimal amount = calculateAverage(financialPeriodService.getEarned(userId, periodType), revenues);
        return new ReportDto(periodType, amount);
    }

    private BigDecimal calculateAverage(BigDecimal sum, List<?> list) {
        if (list == null || list.isEmpty() || sum.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return sum.divide(BigDecimal.valueOf(list.size()), 2, RoundingMode.HALF_UP);
    }

}
