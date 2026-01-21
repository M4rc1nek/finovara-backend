package com.finovara.finovarabackend.reports.service;

import com.finovara.finovarabackend.expense.model.Expense;
import com.finovara.finovarabackend.expense.repository.ExpenseRepository;
import com.finovara.finovarabackend.reports.dto.ReportMonthlyChartDTO;
import com.finovara.finovarabackend.reports.dto.ReportsAverageDTO;
import com.finovara.finovarabackend.reports.dto.ReportsHighestExpense;
import com.finovara.finovarabackend.reports.dto.ReportsSumDTO;
import com.finovara.finovarabackend.revenue.model.Revenue;
import com.finovara.finovarabackend.revenue.repository.RevenueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.BiFunction;

@Service
@RequiredArgsConstructor
public class ReportsService {
    private final RevenueRepository revenueRepository;
    private final ExpenseRepository expenseRepository;

    public List<Revenue> findRevenueForMonth(Long userId, int year, int month) {
        LocalDate from = getMonthBegin(year, month);
        LocalDate to = getMonthEnd(year, month);
        return revenueRepository.findAllByUserAssignedIdAndCreatedAtBetween(userId, from, to);
    }

    public List<Expense> findExpenseForMonth(Long userId, int year, int month) {
        LocalDate from = getMonthBegin(year, month);
        LocalDate to = getMonthEnd(year, month);
        return expenseRepository.findAllByUserAssignedIdAndCreatedAtBetween(userId, from, to);
    }

    public ReportsSumDTO sumRevenueAndExpense(Long userId, int year, int month) {
        List<Revenue> revenues = findRevenueForMonth(userId, year, month);
        List<Expense> expenses = findExpenseForMonth(userId, year, month);

        BigDecimal sumRevenue = revenues.stream()
                .map(Revenue::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal sumExpense = expenses.stream()
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new ReportsSumDTO(sumRevenue, sumExpense);
    }

    public ReportsAverageDTO averageRevenueAndExpense(Long userId, int year, int month) {
        List<Revenue> revenues = findRevenueForMonth(userId, year, month);
        List<Expense> expenses = findExpenseForMonth(userId, year, month);

        BigDecimal sumRevenue = revenues.stream()
                .map(Revenue::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal sumExpense = expenses.stream()
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal avgRevenue = revenues.isEmpty() ? BigDecimal.ZERO : sumRevenue.divide(BigDecimal.valueOf(revenues.size()), 2, RoundingMode.HALF_UP);
        BigDecimal avgExpense = expenses.isEmpty() ? BigDecimal.ZERO : sumExpense.divide(BigDecimal.valueOf(expenses.size()), 2, RoundingMode.HALF_UP);

        return new ReportsAverageDTO(avgRevenue, avgExpense);
    }

    public List<ReportsHighestExpense> highestExpense(Long userId, int year, int month) {
        List<Expense> expenses = findExpenseForMonth(userId, year, month);

        return expenses.stream()
                .sorted(Comparator.comparing(Expense::getAmount).reversed())
                .limit(3)
                .map(exp -> new ReportsHighestExpense(
                        exp.getCategory(),
                        exp.getAmount()
                ))
                .toList();
    }

    public ReportsSumDTO sumRevenueAndExpense(Long userId) {
        LocalDate now = LocalDate.now();
        return sumRevenueAndExpense(userId, now.getYear(), now.getMonthValue());
    }

    public ReportsAverageDTO averageRevenueAndExpense(Long userId) {
        LocalDate now = LocalDate.now();
        return averageRevenueAndExpense(userId, now.getYear(), now.getMonthValue());
    }

    public List<ReportsHighestExpense> highestExpense(Long userId) {
        LocalDate now = LocalDate.now();
        return highestExpense(userId, now.getYear(), now.getMonthValue());
    }

    public List<ReportMonthlyChartDTO> getMonthlyChart(Long userId) {
        LocalDate now = LocalDate.now();
        int daysInMonth = now.lengthOfMonth();
        List<ReportMonthlyChartDTO> chartData = new ArrayList<>();

        for (int day = 1; day <= daysInMonth; day++) {
            // Tworzysz datę dla konkretnego dnia
            LocalDate targetDate = LocalDate.of(now.getYear(), now.getMonthValue(), day);


            BigDecimal dayIncome = revenueRepository.sumRevenueForDay(userId, targetDate);
            BigDecimal dayExpense = expenseRepository.sumExpenseForDay(userId, targetDate);

            chartData.add(new ReportMonthlyChartDTO(day, dayIncome, dayExpense));
        }
        return chartData;
    }


    private LocalDate getMonthBegin(int year, int month) {
        return LocalDate.of(year, month, 1);
    }

    private LocalDate getMonthEnd(int year, int month) {
        LocalDate start = getMonthBegin(year, month);
        return start.withDayOfMonth(start.lengthOfMonth());
    }

}
