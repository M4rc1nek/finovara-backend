package com.finovara.financeservice.limit.service;

import com.finovara.contracts.util.PeriodType;
import com.finovara.financeservice.expense.repository.ExpenseRepository;
import com.finovara.financeservice.limit.dto.LimitSpendingDto;
import com.finovara.financeservice.limit.model.Limit;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class LimitSpendingService {

    private final ExpenseRepository expenseRepository;

    public Map<Long, BigDecimal> calculateSpent(List<Limit> limits, LocalDate today) {
        Map<Long, BigDecimal> spendingByLimitId = new HashMap<>();
        for (PeriodType period : PeriodType.values()) {
            addSpendingForPeriod(limits, period, today, spendingByLimitId);
        }
        return spendingByLimitId;
    }

    private void addSpendingForPeriod(List<Limit> limits, PeriodType period, LocalDate today, Map<Long, BigDecimal> spendingByLimitId) {
        List<Long> limitIds = limits.stream()
                .filter(limit -> limit.getPeriodType() == period)
                .map(Limit::getId)
                .toList();
        if (limitIds.isEmpty()) {
            return;
        }

        List<LimitSpendingDto> spending = expenseRepository.sumExpensesByLimitIds(
                limitIds, period.getStartDate(today), today);
        spending.forEach(result -> spendingByLimitId.put(
                result.limitId(), result.amount() == null ? BigDecimal.ZERO : result.amount()));
    }
}
