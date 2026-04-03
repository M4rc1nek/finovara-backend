package com.finovara.finovarabackend.util.service.periodbalance;

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
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FinancialPeriodService {

    private final ExpenseRepository expenseRepository;
    private final RevenueRepository revenueRepository;

    public BigDecimal getExpensesSum(Long userId, PeriodType period) {
        LocalDate today = LocalDate.now();
        LocalDate from = period.getStartDate(today);
        return calculateExpenseInPeriod(userId, from, today);
    }

    public BigDecimal getRevenueSum(Long userId, PeriodType period) {
        LocalDate today = LocalDate.now();
        LocalDate from = period.getStartDate(today);
        return calculateRevenueInPeriod(userId, from, today);
    }

    public List<Revenue> getRevenuesInPeriod(Long userId, PeriodType period) {
        LocalDate today = LocalDate.now();
        LocalDate from = period.getStartDate(today);
        return revenueRepository.findAllByUserAssignedIdAndCreatedAtBetween(userId, from, today);
    }

    public List<Expense> getExpensesInPeriod(Long userId, PeriodType period) {
        LocalDate today = LocalDate.now();
        LocalDate from = period.getStartDate(today);
        return expenseRepository.findAllByUserAssignedIdAndCreatedAtBetween(userId, from, today);
    }

    public List<Revenue> getRevenuesInPeriodByCategory(Long userId, PeriodType period, RevenueCategory category) {
        LocalDate today = LocalDate.now();
        LocalDate from = period.getStartDate(today);
        return revenueRepository.findAllByUserAssignedIdAndCreatedAtBetweenAndCategory(userId, from, today, category);
    }

    public List<Expense> getExpensesInPeriodByCategory(Long userId, PeriodType period, ExpenseCategory category) {
        LocalDate today = LocalDate.now();
        LocalDate from = period.getStartDate(today);
        return expenseRepository.findAllByUserAssignedIdAndCreatedAtBetweenAndCategory(userId, from, today, category);
    }

    private BigDecimal calculateExpenseInPeriod(Long userId, LocalDate from, LocalDate to) {
        BigDecimal spent = expenseRepository.sumExpensesByUserAndDateRange(userId, from, to);
        return spent != null ? spent : BigDecimal.ZERO;
    }

    private BigDecimal calculateRevenueInPeriod(Long userId, LocalDate from, LocalDate to) {
        BigDecimal revenue = revenueRepository.sumRevenuesByUserAndDateRange(userId, from, to);
        return revenue != null ? revenue : BigDecimal.ZERO;
    }
}