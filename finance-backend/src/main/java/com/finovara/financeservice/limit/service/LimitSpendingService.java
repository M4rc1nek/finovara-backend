package com.finovara.financeservice.limit.service;

import com.finovara.contracts.util.PeriodType;
import com.finovara.financeservice.expense.repository.ExpenseRepository;
import com.finovara.financeservice.internal.report.util.dto.ExpenseCategorySumDto;
import com.finovara.financeservice.internal.report.util.dto.UserExpenseCategorySumDto;
import com.finovara.financeservice.limit.model.Limit;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
                List<ExpenseCategorySumDto> sums = expenseRepository.sumExpensesGroupedByCategory(
                        userId, period.getStartDate(today), today);
                sumsByPeriod.put(period, sums);
            }
            spentByLimitId.put(limit.getId(), sumForLimit(limit, sumsByPeriod.get(period)));
        }
        return spentByLimitId;
    }

    public Map<Long, Map<Long, BigDecimal>> calculateSpent(
            Map<Long, List<Limit>> limitsByUserId, LocalDate today) {
        Map<PeriodType, Set<Long>> userIdsByPeriod = new HashMap<>();
        for (Map.Entry<Long, List<Limit>> entry : limitsByUserId.entrySet()) {
            for (Limit limit : entry.getValue()) {
                userIdsByPeriod.computeIfAbsent(limit.getPeriodType(), ignored -> new HashSet<>()).add(entry.getKey());
            }
        }

        Map<Long, Map<Long, BigDecimal>> spentByUserAndLimit = new HashMap<>();
        for (Map.Entry<PeriodType, Set<Long>> periodEntry : userIdsByPeriod.entrySet()) {
            List<Long> userIds = periodEntry.getValue().stream().sorted().toList();
            List<UserExpenseCategorySumDto> sums = expenseRepository.sumExpensesGroupedByUserAndCategory(
                    userIds, periodEntry.getKey().getStartDate(today), today);
            Map<Long, List<UserExpenseCategorySumDto>> sumsByUserId = sums.stream()
                    .collect(Collectors.groupingBy(UserExpenseCategorySumDto::userId));

            for (Long userId : userIds) {
                List<UserExpenseCategorySumDto> userSums = sumsByUserId.getOrDefault(userId, List.of());
                for (Limit limit : limitsByUserId.get(userId)) {
                    if (limit.getPeriodType() == periodEntry.getKey()) {
                        spentByUserAndLimit.computeIfAbsent(userId, ignored -> new HashMap<>())
                                .put(limit.getId(), sumForUserLimit(limit, userSums));
                    }
                }
            }
        }
        return spentByUserAndLimit;
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

    private BigDecimal sumForUserLimit(Limit limit, List<UserExpenseCategorySumDto> categorySums) {
        BigDecimal total = BigDecimal.ZERO;
        for (UserExpenseCategorySumDto categorySum : categorySums) {
            if (limit.getCategory() == null || limit.getCategory() == categorySum.category()) {
                total = total.add(categorySum.amount());
            }
        }
        return total;
    }
}
