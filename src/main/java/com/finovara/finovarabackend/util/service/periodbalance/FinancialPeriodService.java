package com.finovara.finovarabackend.util.service.periodbalance;

import com.finovara.finovarabackend.expense.repository.ExpenseRepository;
import com.finovara.finovarabackend.revenue.repository.RevenueRepository;
import com.finovara.finovarabackend.util.model.PeriodType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class FinancialPeriodService {

    private final ExpenseRepository expenseRepository;
    private final RevenueRepository revenueRepository;

    public BigDecimal getSpent(Long userId, PeriodType period) {
        LocalDate today = LocalDate.now();
        LocalDate from = getStartDate(today, period);
        return getExpensesInPeriod(userId, from, today);
    }

    public BigDecimal getEarned(Long userId, PeriodType period) {
        LocalDate today = LocalDate.now();
        LocalDate from = getStartDate(today, period);
        return getRevenuesInPeriod(userId, from, today);
    }

    private LocalDate getStartDate(LocalDate today, PeriodType period) {
        return switch (period) {
            case DAILY -> today;
            case WEEKLY -> today.with(DayOfWeek.MONDAY);
            case MONTHLY -> today.withDayOfMonth(1);
        };
    }

    private BigDecimal getExpensesInPeriod(Long userId, LocalDate from, LocalDate to) {
        BigDecimal spent = expenseRepository.sumExpensesByUserAndDateRange(userId, from, to);
        return spent != null ? spent : BigDecimal.ZERO;
    }

    private BigDecimal getRevenuesInPeriod(Long userId, LocalDate from, LocalDate to) {
        BigDecimal revenue = revenueRepository.sumRevenuesByUserAndDateRange(userId, from, to);
        return revenue != null ? revenue : BigDecimal.ZERO;
    }
}