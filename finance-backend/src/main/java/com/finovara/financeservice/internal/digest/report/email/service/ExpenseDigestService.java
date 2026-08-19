package com.finovara.financeservice.internal.digest.report.email.service;

import com.finovara.contracts.model.PeriodType;
import com.finovara.contracts.model.transaction.ExpenseCategory;
import com.finovara.contracts.percentage.CalculatePercentage;
import com.finovara.financeservice.expense.model.Expense;
import com.finovara.financeservice.expense.repository.ExpenseRepository;
import com.finovara.financeservice.internal.digest.report.email.dto.ExpenseSummary;
import com.finovara.financeservice.limit.model.Limit;
import com.finovara.financeservice.limit.repository.LimitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExpenseDigestService {

    private final ExpenseRepository expenseRepository;
    private final LimitRepository limitRepository;

    public ExpenseSummary calculateSummary(Long userId, LocalDate from, LocalDate to) {
        BigDecimal sum = calculateSum(userId, from, to);
        String topCategory = findTopCategory(userId, from, to);
        Optional<Expense> highestExpense = findHighestExpense(userId, from, to);
        int daysWithoutExpense = calculateDaysWithoutExpense(userId, from, to);
        BigDecimal remainingBudgetPercentage = calculateRemainingMonthlyBudget(userId);

        return new ExpenseSummary(
                sum,
                topCategory,
                highestExpense.map(Expense::getAmount).orElse(BigDecimal.ZERO),
                highestExpense.map(e -> e.getCategory().name()).orElse(null),
                highestExpense.map(Expense::getCreatedAt).orElse(null),
                daysWithoutExpense,
                remainingBudgetPercentage
        );
    }

    private BigDecimal calculateSum(Long userId, LocalDate from, LocalDate to) {
        return expenseRepository.sumExpensesByUserAndDateRange(userId, from, to)
                .orElse(BigDecimal.ZERO);
    }

    private String findTopCategory(Long userId, LocalDate from, LocalDate to) {
        return expenseRepository
                .findTopExpenseCategoriesByUserIdAndPeriod(userId, from, to, PageRequest.of(0, 1))
                .stream()
                .findFirst()
                .map(ExpenseCategory::name)
                .orElse(null);
    }

    private Optional<Expense> findHighestExpense(Long userId, LocalDate from, LocalDate to) {
        return expenseRepository
                .findTopExpensesByUserIdAndPeriod(userId, from, to, PageRequest.of(0, 1))
                .stream()
                .findFirst();
    }

    private int calculateDaysWithoutExpense(Long userId, LocalDate from, LocalDate to) {
        List<Expense> expensesInPeriod = expenseRepository.findAllByUserIdAndCreatedAtBetween(userId, from, to);

        Set<LocalDate> daysWithExpense = expensesInPeriod.stream()
                .map(Expense::getCreatedAt)
                .collect(Collectors.toSet());

        int daysInPeriod = (int) ChronoUnit.DAYS.between(from, to) + 1;

        return daysInPeriod - daysWithExpense.size();
    }

    private BigDecimal calculateRemainingMonthlyBudget(Long userId) {
        LocalDate today = LocalDate.now();
        LocalDate monthStart = today.withDayOfMonth(1);

        Optional<Limit> limit = limitRepository.findGeneralLimit(userId, PeriodType.MONTHLY);
        if (limit.isEmpty()) {
            return BigDecimal.ZERO;
        }

        BigDecimal spentThisMonth = expenseRepository.sumExpensesByUserAndDateRange(userId, monthStart, today)
                .orElse(BigDecimal.ZERO);
        BigDecimal spentPercentage = CalculatePercentage.calculatePercentage(spentThisMonth, limit.get().getAmount());

        return BigDecimal.valueOf(100).subtract(spentPercentage).max(BigDecimal.ZERO);
    }
}