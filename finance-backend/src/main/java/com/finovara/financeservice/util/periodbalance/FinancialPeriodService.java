package com.finovara.financeservice.util.periodbalance;

import com.finovara.contracts.model.transaction.ExpenseCategory;
import com.finovara.contracts.model.transaction.RevenueCategory;
import com.finovara.contracts.model.PeriodType;
import com.finovara.financeservice.expense.model.Expense;
import com.finovara.financeservice.expense.repository.ExpenseRepository;
import com.finovara.financeservice.revenue.model.Revenue;
import com.finovara.financeservice.revenue.repository.RevenueRepository;
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
        return expenseRepository.findAllByUserIdAndCreatedAtBetweenAndCategory(userId, from, today, category);
    }

    public List<Revenue> getRevenuesInPeriodByCategory(Long userId, PeriodType period, RevenueCategory category) {
        LocalDate today = LocalDate.now();
        LocalDate from = period.getStartDate(today);
        return revenueRepository.findAllByUserIdAndCreatedAtBetweenAndCategory(userId, from, today, category);
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
        return expenseRepository.avgExpensesByUserIdAndPeriod(userId, from, to).orElse(BigDecimal.ZERO);
    }

    private BigDecimal calculateAverageRevenueInPeriod(Long userId, PeriodType periodType) {
        LocalDate to = LocalDate.now();
        LocalDate from = periodType.getStartDate(to);
        return revenueRepository.avgRevenuesByUserIdAndPeriod(userId, from, to).orElse(BigDecimal.ZERO);
    }
}