package com.finovara.financeservice.internal.digest.email;

import com.finovara.contracts.model.PeriodType;
import com.finovara.contracts.percentage.CalculatePercentage;
import com.finovara.financeservice.expense.model.Expense;
import com.finovara.financeservice.expense.repository.ExpenseRepository;
import com.finovara.financeservice.limit.model.Limit;
import com.finovara.financeservice.limit.repository.LimitRepository;
import com.finovara.financeservice.revenue.repository.RevenueRepository;
import com.finovara.financeservice.settings.finances.recurring.model.RecurringSettings;
import com.finovara.financeservice.settings.finances.recurring.repository.RecurringSettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InternalDigestEmailService {

    private final ExpenseRepository expenseRepository;
    private final RevenueRepository revenueRepository;
    private final LimitRepository limitRepository;
    private final RecurringSettingsRepository recurringSettingsRepository;

    public BigDecimal sumExpenses(Long userId) {
        LocalDate to = LocalDate.now();
        LocalDate from = getWeeklyPeriod();
        return expenseRepository.sumExpensesByUserAndDateRange(userId, from, to).orElse(BigDecimal.ZERO);
    }

    public BigDecimal getHighestExpense(Long userId) {
        LocalDate to = LocalDate.now();
        LocalDate from = getWeeklyPeriod();
        return expenseRepository.findTopExpenseByUserIdAndPeriod(userId, from, to).orElse(BigDecimal.ZERO);
    }

    public BigDecimal sumRevenues(Long userId) {
        LocalDate to = LocalDate.now();
        LocalDate from = getWeeklyPeriod();
        return revenueRepository.sumRevenuesByUserAndDateRange(userId, from, to).orElse(BigDecimal.ZERO);
    }

    public BigDecimal getHighestRevenue(Long userId) {
        LocalDate to = LocalDate.now();
        LocalDate from = getWeeklyPeriod();
        return revenueRepository.findTopRevenueByUserIdAndPeriod(userId, from, to).orElse(BigDecimal.ZERO);
    }

    public BigDecimal getRemainingMonthlyBudget(Long userId) {
        LocalDate today = LocalDate.now();
        LocalDate monthStart = today.withDayOfMonth(1);

        Limit limit = limitRepository.findGeneralLimit(userId, PeriodType.MONTHLY).orElse(null);

        if (limit == null) {
            return BigDecimal.ZERO;
        }

        BigDecimal spent = expenseRepository.sumExpensesByUserAndDateRange(userId, monthStart, today)
                .orElse(BigDecimal.ZERO);

        BigDecimal spentPercentage = CalculatePercentage.calculatePercentage(spent, limit.getAmount());

        return BigDecimal.valueOf(100).subtract(spentPercentage).max(BigDecimal.ZERO);
    }

    public int daysWithoutExpense(Long userId) {
        LocalDate to = LocalDate.now();
        LocalDate from = getWeeklyPeriod();

        List<Expense> expenses =
                expenseRepository.findAllByUserIdAndCreatedAtBetween(userId, from, to);

        Set<LocalDate> daysWithExpense = expenses.stream()
                .map(Expense::getCreatedAt)
                .collect(Collectors.toSet());

        int daysInPeriod = (int) ChronoUnit.DAYS.between(from, to) + 1;

        return daysInPeriod - daysWithExpense.size();
    }

    public BigDecimal savedMoney(Long userId){
        LocalDate to = LocalDate.now();
        LocalDate from = getWeeklyPeriod();

        BigDecimal spent = expenseRepository.sumExpensesByUserAndDateRange(userId, from, to).orElse(BigDecimal.ZERO);
        BigDecimal earned = revenueRepository.sumRevenuesByUserAndDateRange(userId, from, to).orElse(BigDecimal.ZERO);

        return (earned.subtract(spent));
    }

    public List<RecurringSettings> getUpcomingRecurringPayments(Long userId) {
        LocalDate from = LocalDate.now();
        LocalDate to = from.plusDays(7);
        return recurringSettingsRepository.findUpcomingByUserId(userId, from, to);
    }

    private LocalDate getWeeklyPeriod() {
        return LocalDate.now().minusDays(6);
    }


}
