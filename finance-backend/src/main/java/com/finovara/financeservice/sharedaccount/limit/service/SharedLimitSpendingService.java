package com.finovara.financeservice.sharedaccount.limit.service;

import com.finovara.contracts.util.PeriodType;
import com.finovara.financeservice.internal.report.util.dto.ExpenseCategorySumDto;
import com.finovara.financeservice.sharedaccount.expense.repository.SharedExpenseRepository;
import com.finovara.financeservice.sharedaccount.limit.model.SharedLimit;
import com.finovara.financeservice.sharedaccount.participants.SharedAccountParticipantsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class SharedLimitSpendingService {

    private final SharedExpenseRepository sharedExpenseRepository;

    public Map<Long, BigDecimal> calculateSpent(SharedAccountParticipantsResponse participants,
                                                List<SharedLimit> limits, LocalDate today) {
        Map<PeriodType, List<ExpenseCategorySumDto>> sumsByPeriod = new HashMap<>();
        Map<Long, BigDecimal> spentByLimitId = new HashMap<>();

        for (SharedLimit limit : limits) {
            List<ExpenseCategorySumDto> sums = sumsByPeriod.computeIfAbsent(limit.getPeriodType(),
                    period -> sharedExpenseRepository.sumExpensesGroupedByCategoryForOwnerIdOrMemberId(
                            participants.ownerId(), participants.memberId(), period.getStartDate(today), today));

            spentByLimitId.put(limit.getId(), sumForLimit(limit, sums));
        }
        return spentByLimitId;
    }

    private BigDecimal sumForLimit(SharedLimit limit, List<ExpenseCategorySumDto> categorySums) {
        BigDecimal total = BigDecimal.ZERO;
        for (ExpenseCategorySumDto categorySum : categorySums) {
            if (limit.getCategory() == null || limit.getCategory() == categorySum.category()) {
                total = total.add(categorySum.amount());
            }
        }
        return total;
    }
}