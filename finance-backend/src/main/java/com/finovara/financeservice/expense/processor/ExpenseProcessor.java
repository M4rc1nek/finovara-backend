package com.finovara.financeservice.expense.processor;

import com.finovara.contracts.mainaccount.transaction.event.limit.LimitStatsEvent;
import com.finovara.contracts.outbox.OutboxService;
import com.finovara.financeservice.expense.model.Expense;
import com.finovara.financeservice.expense.repository.ExpenseRepository;
import com.finovara.financeservice.limit.dto.LimitStatsDto;
import com.finovara.financeservice.limit.model.Limit;
import com.finovara.financeservice.limit.repository.LimitRepository;
import com.finovara.financeservice.limit.service.LimitCalculateService;
import com.finovara.financeservice.trash.service.TrashMoverService;
import com.finovara.financeservice.wallet.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ExpenseProcessor {

    private static final String LIMIT_STATS_TOPIC = "limit.calculate-stats";

    private final ExpenseRepository expenseRepository;
    private final LimitRepository limitRepository;
    private final LimitCalculateService limitCalculateService;
    private final WalletService walletService;
    private final OutboxService outboxService;
    private final TrashMoverService trashMoverService;

    @Transactional
    @CacheEvict(value = "expense:suggestion", allEntries = true)
    public void moveAllExpensesToTrash() {
        List<Long> userIds = expenseRepository.findUserIdsWithActiveExpenses().stream().sorted().toList();
        if (userIds.isEmpty()) {
            log.info("Moved 0 expenses to trash");
            return;
        }

        walletService.lockWalletsForUpdate(userIds);
        List<Expense> expenses = expenseRepository.findAllActiveByUserIdInForUpdate(userIds);
        Map<Long, List<Limit>> limitsByUserId = limitRepository.findAllActiveByUserIdIn(userIds).stream().collect(Collectors.groupingBy(Limit::getUserId));

        trashMoverService.moveExpensesToTrash(expenses);
        Map<Long, List<LimitStatsDto>> statsByUserId = limitCalculateService.calculateLimitStatsForUsers(limitsByUserId, LocalDate.now());

        statsByUserId.forEach((userId, stats) -> stats.forEach(stat ->
                outboxService.save("Limit", stat.limitId().toString(), LIMIT_STATS_TOPIC,
                        new LimitStatsEvent(userId, stat.limitId(), stat.percentage(), stat.periodType()))));
        log.info("Moved {} expenses to trash", expenses.size());
    }
}
