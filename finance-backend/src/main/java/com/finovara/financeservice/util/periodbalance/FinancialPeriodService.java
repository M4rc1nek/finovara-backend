package com.finovara.financeservice.util.periodbalance;

import com.finovara.contracts.util.PeriodType;
import com.finovara.contracts.util.model.ExpenseCategory;
import com.finovara.contracts.util.model.RevenueCategory;
import com.finovara.financeservice.expense.model.Expense;
import com.finovara.financeservice.expense.repository.ExpenseRepository;
import com.finovara.financeservice.revenue.model.Revenue;
import com.finovara.financeservice.revenue.repository.RevenueRepository;
import com.finovara.financeservice.sharedaccount.expense.repository.SharedExpenseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FinancialPeriodService {

    private final ExpenseRepository expenseRepository;
    private final SharedExpenseRepository sharedExpenseRepository;
    private final RevenueRepository revenueRepository;

    @Value("${finance.pagination.page-size}")
    private int pageSize;

    public List<Expense> getExpensesInPeriodByCategory(Long userId, PeriodType period, ExpenseCategory category, int page) {
        LocalDate today = LocalDate.now();
        LocalDate from = period.getStartDate(today);
        return expenseRepository.findAllByUserIdAndCreatedAtBetweenAndCategory(userId, from, today, category, PageRequest.of(page, pageSize));
    }

    public List<Revenue> getRevenuesInPeriodByCategory(Long userId, PeriodType period, RevenueCategory category, int page) {
        LocalDate today = LocalDate.now();
        LocalDate from = period.getStartDate(today);
        return revenueRepository.findAllByUserIdAndCreatedAtBetweenAndCategory(userId, from, today, category, PageRequest.of(page, pageSize));
    }

    public BigDecimal getExpensesSum(Long userId, PeriodType period, ExpenseCategory category) {
        LocalDate to = LocalDate.now();
        LocalDate from = period.getStartDate(to);
        if (category == null) {
            return expenseRepository.sumExpensesByUserAndDateRange(userId, from, to).orElse(BigDecimal.ZERO);
        }
        return expenseRepository.sumExpensesByUserAndDateRangeAndCategory(userId, from, to, category).orElse(BigDecimal.ZERO);
    }

    public BigDecimal getSharedExpensesSum(Long userId, PeriodType period, ExpenseCategory category) {
        LocalDate to = LocalDate.now();
        LocalDate from = period.getStartDate(to);
        if (category == null) {
            return sharedExpenseRepository.sumExpensesByUsersAndDateRange(userId, from, to).orElse(BigDecimal.ZERO);
        }
        return sharedExpenseRepository.sumExpensesByUsersAndDateRangeAndCategory(userId, from, to, category).orElse(BigDecimal.ZERO);
    }
}
