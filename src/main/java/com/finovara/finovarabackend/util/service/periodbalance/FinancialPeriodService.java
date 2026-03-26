package com.finovara.finovarabackend.util.service.periodbalance;

import com.finovara.finovarabackend.expense.repository.ExpenseRepository;
import com.finovara.finovarabackend.revenue.repository.RevenueRepository;
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

    public LocalDate today() {
        return LocalDate.now();
    }

    public BigDecimal getSummedExpenseToday(Long userId) {
        LocalDate today = today();
        return getExpensesInPeriod(userId, today, today);
    }

    public BigDecimal getSummedExpenseWeekly(Long userId) {
        LocalDate today = today();
        LocalDate firstDayOfWeek = today.with(DayOfWeek.MONDAY);
        return getExpensesInPeriod(userId, firstDayOfWeek, today);
    }

    public BigDecimal getSummedExpenseMonthly(Long userId) {
        LocalDate today = today();
        LocalDate startMonth = today.withDayOfMonth(1);
        return getExpensesInPeriod(userId, startMonth, today);
    }

    public BigDecimal getSummedRevenuesToday(Long userId){
        LocalDate today = today();
        return getRevenuesInPeriod(userId, today, today);
    }

    public BigDecimal getSummedRevenuesWeekly(Long userId) {
        LocalDate today = today();
        LocalDate firstDayOfWeek = today.with(DayOfWeek.MONDAY);
        return getRevenuesInPeriod(userId, firstDayOfWeek, today);
    }

    public BigDecimal getSummedRevenuesMonthly(Long userId) {
        LocalDate today = today();
        LocalDate startMonth = today.withDayOfMonth(1);
        return getRevenuesInPeriod(userId, startMonth, today);
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
