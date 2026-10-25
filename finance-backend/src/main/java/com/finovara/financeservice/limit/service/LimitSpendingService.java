package com.finovara.financeservice.limit.service;

import com.finovara.contracts.util.PeriodType;
import com.finovara.financeservice.expense.repository.ExpenseRepository;
import com.finovara.financeservice.internal.report.util.dto.ExpenseCategorySumDto;
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

    public Map<Long, BigDecimal> calculateSpent(Long userId, List<Limit> limits, LocalDate today) {
        Map<PeriodType, List<ExpenseCategorySumDto>> sumsByPeriod = new HashMap<>();
        Map<Long, BigDecimal> spentByLimitId = new HashMap<>();

        for (Limit limit : limits) {
            PeriodType period = limit.getPeriodType();

            if (!sumsByPeriod.containsKey(period)) {
                List<ExpenseCategorySumDto> sums = expenseRepository.sumExpensesGroupedByCategory(userId, period.getStartDate(today), today);
                sumsByPeriod.put(period, sums);
            }

            spentByLimitId.put(limit.getId(), sumForLimit(limit, sumsByPeriod.get(period)));
        }
        return spentByLimitId;
    }

    private BigDecimal sumForLimit(Limit limit, List<ExpenseCategorySumDto> categorySums) {
        BigDecimal total = BigDecimal.ZERO;
        for (ExpenseCategorySumDto categorySum : categorySums) {
            if (limit.getCategory() == null || limit.getCategory() == categorySum.category()) {
                total = total.add(categorySum.amount());
            }
        }
        return total;
    }
}