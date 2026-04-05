package com.finovara.finovarabackend.util.periodbalance;

import com.finovara.finovarabackend.expense.model.Expense;
import com.finovara.finovarabackend.expense.model.ExpenseCategory;
import com.finovara.finovarabackend.expense.repository.ExpenseRepository;
import com.finovara.finovarabackend.revenue.model.Revenue;
import com.finovara.finovarabackend.revenue.model.RevenueCategory;
import com.finovara.finovarabackend.revenue.repository.RevenueRepository;
import com.finovara.finovarabackend.util.model.PeriodType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FinancialPeriodService {

    private final ExpenseRepository expenseRepository;
    private final RevenueRepository revenueRepository;

    public List<Expense> getExpensesInPeriodByCategory(Long userId, PeriodType period, ExpenseCategory category) {
        LocalDate today = LocalDate.now();
        LocalDate from = period.getStartDate(today);
        return expenseRepository.findAllByUserAssignedIdAndCreatedAtBetweenAndCategory(userId, from, today, category);
    }

    public List<Revenue> getRevenuesInPeriodByCategory(Long userId, PeriodType period, RevenueCategory category) {
        LocalDate today = LocalDate.now();
        LocalDate from = period.getStartDate(today);
        return revenueRepository.findAllByUserAssignedIdAndCreatedAtBetweenAndCategory(userId, from, today, category);
    }

    public BigDecimal getExpensesSum(Long userId, PeriodType period) {
        return calculateSumExpenseInPeriod(userId, period);
    }

    public BigDecimal getRevenueSum(Long userId, PeriodType period) {
        return calculateSumRevenueInPeriod(userId, period);
    }

    public BigDecimal getAverageExpense(Long userId, PeriodType periodType) {
        return calculateAverageExpenseInPeriod(userId, periodType);
    }

    public BigDecimal getAverageRevenue(Long userId, PeriodType periodType) {
        return calculateAverageRevenueInPeriod(userId, periodType);
    }

    private BigDecimal calculateSumExpenseInPeriod(Long userId, PeriodType periodType) {
        LocalDate to = LocalDate.now();
        LocalDate from = periodType.getStartDate(to);
        return expenseRepository.sumExpensesByUserAndDateRange(userId, from, to).orElse(BigDecimal.ZERO);
    }

    private BigDecimal calculateSumRevenueInPeriod(Long userId, PeriodType periodType) {
        LocalDate to = LocalDate.now();
        LocalDate from = periodType.getStartDate(to);
        return revenueRepository.sumRevenuesByUserAndDateRange(userId, from, to).orElse(BigDecimal.ZERO);
    }

    private BigDecimal calculateAverageExpenseInPeriod(Long userId, PeriodType periodType) {
        LocalDate to = LocalDate.now();
        LocalDate from = periodType.getStartDate(to);
        return expenseRepository.avgExpensesByUserAssignedIdAndPeriod(userId, from, to).orElse(BigDecimal.ZERO);
    }

    private BigDecimal calculateAverageRevenueInPeriod(Long userId, PeriodType periodType) {
        LocalDate to = LocalDate.now();
        LocalDate from = periodType.getStartDate(to);
        return revenueRepository.avgRevenuesByUserAssignedIdAndPeriod(userId, from, to).orElse(BigDecimal.ZERO);
    }
}