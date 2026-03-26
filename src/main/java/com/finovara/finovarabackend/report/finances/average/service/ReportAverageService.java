package com.finovara.finovarabackend.report.finances.average.service;

import com.finovara.finovarabackend.expense.model.Expense;
import com.finovara.finovarabackend.expense.repository.ExpenseRepository;
import com.finovara.finovarabackend.report.dto.ReportDto;
import com.finovara.finovarabackend.report.model.ReportPeriodType;
import com.finovara.finovarabackend.revenue.model.Revenue;
import com.finovara.finovarabackend.revenue.repository.RevenueRepository;
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

    public ReportDto calculateAverageExpense(Long userId, ReportPeriodType reportPeriodType) {
        List<Expense> expenses = expenseRepository.findAllByUserAssignedId(userId);

        BigDecimal amount = switch (reportPeriodType) {
            case DAILY -> calculateAverage(financialPeriodService.getSummedExpenseToday(userId), expenses);
            case WEEKLY -> calculateAverage(financialPeriodService.getSummedExpenseWeekly(userId), expenses);
            case MONTHLY -> calculateAverage(financialPeriodService.getSummedExpenseMonthly(userId), expenses);
        };

        return new ReportDto(reportPeriodType, amount);
    }

    public ReportDto calculateAverageRevenue(Long userId, ReportPeriodType reportPeriodType) {
        List<Revenue> revenues = revenueRepository.findAllByUserAssignedId(userId);
        BigDecimal amount = switch (reportPeriodType) {
            case DAILY -> calculateAverage(financialPeriodService.getSummedRevenuesToday(userId), revenues);
            case WEEKLY -> calculateAverage(financialPeriodService.getSummedRevenuesWeekly(userId), revenues);
            case MONTHLY -> calculateAverage(financialPeriodService.getSummedRevenuesMonthly(userId), revenues);
        };
        return new ReportDto(reportPeriodType, amount);
    }

    private BigDecimal calculateAverage(BigDecimal sum, List<?> list) {
        if (list == null || list.isEmpty() || sum.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return sum.divide(BigDecimal.valueOf(list.size()), 2, RoundingMode.HALF_UP);
    }

}
