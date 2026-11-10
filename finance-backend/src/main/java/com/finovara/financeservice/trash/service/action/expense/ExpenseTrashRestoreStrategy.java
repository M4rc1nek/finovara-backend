package com.finovara.financeservice.trash.service.action.expense;

import com.finovara.contracts.exception.notfound.RequestedEntityNotFoundException;
import com.finovara.contracts.mainaccount.activity.event.expense.ExpenseActivityEvent;
import com.finovara.contracts.mainaccount.activity.model.ExpenseActivityType;
import com.finovara.contracts.mainaccount.activity.model.TransactionType;
import com.finovara.contracts.mainaccount.activity.model.WalletTransactionReason;
import com.finovara.contracts.mainaccount.transaction.event.limit.LimitStatsEvent;
import com.finovara.contracts.outbox.OutboxService;
import com.finovara.financeservice.expense.model.Expense;
import com.finovara.financeservice.expense.repository.ExpenseRepository;
import com.finovara.financeservice.limit.dto.LimitStatsDto;
import com.finovara.financeservice.limit.model.Limit;
import com.finovara.financeservice.limit.repository.LimitRepository;
import com.finovara.financeservice.limit.service.LimitCalculateService;
import com.finovara.financeservice.settings.piggybank.autopayments.model.PiggyBankAutomationMode;
import com.finovara.financeservice.settings.piggybank.roundup.service.RoundUpService;
import com.finovara.financeservice.trash.model.TrashItem;
import com.finovara.financeservice.trash.model.TrashEntityType;
import com.finovara.financeservice.trash.service.TrashRestoreStrategy;
import com.finovara.financeservice.wallet.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ExpenseTrashRestoreStrategy implements TrashRestoreStrategy {

    private static final String LIMIT_STATS_TOPIC = "limit.calculate-stats";

    private final ExpenseRepository expenseRepository;
    private final LimitRepository limitRepository;
    private final LimitCalculateService limitCalculateService;
    private final WalletService walletService;
    private final RoundUpService roundUpService;
    private final OutboxService outboxService;

    @Override
    public boolean supports(TrashEntityType entityType) {
        return entityType == TrashEntityType.EXPENSE;
    }

    @Override
    public void restore(TrashItem item) {
        Expense expense = getExpense(item);
        restoreExpense(expense);
        restoreFinancialEffects(item, expense);
        publishExpenseCreatedEvent(item, expense);
        publishLimitStatsEvents(item.getUserId());
    }

    private Expense getExpense(TrashItem item) {
        return expenseRepository.findTrashedByIdAndUserIdForUpdate(item.getEntityId(), item.getUserId())
                .orElseThrow(() -> new RequestedEntityNotFoundException("Expense not found in trash"));
    }

    private void restoreExpense(Expense expense) {
        expense.setDeletedAt(null);
        expenseRepository.save(expense);
    }

    private void restoreFinancialEffects(TrashItem item, Expense expense) {
        if (!Boolean.TRUE.equals(item.getFinancialEffectsReverted())) {
            return;
        }
        walletService.removeBalanceFromWallet(item.getUserId(), expense.getAmount(),
                TransactionType.WITHDRAWAL, WalletTransactionReason.EXPENSE);
        roundUpService.handleExpenseForRoundUp(item.getUserId(), expense.getId(), PiggyBankAutomationMode.APPLY);
    }

    private void publishExpenseCreatedEvent(TrashItem item, Expense expense) {
        outboxService.save("Expense", expense.getId().toString(), "expense.created",
                new ExpenseActivityEvent(item.getUserId(), ExpenseActivityType.ADDED_EXPENSE, expense.getAmount(),
                        expense.getCategory(), null, null, LocalDateTime.now()));
    }

    private void publishLimitStatsEvents(Long userId) {
        List<Limit> limits = limitRepository.findAllActiveByUserId(userId);
        List<LimitStatsDto> stats = limitCalculateService.calculateLimitStats(limits, userId, LocalDate.now());
        for (LimitStatsDto limitStats : stats) {
            LimitStatsEvent event = new LimitStatsEvent(userId, limitStats.limitId(), limitStats.percentage(),
                    limitStats.periodType());
            outboxService.save("Limit", limitStats.limitId().toString(), LIMIT_STATS_TOPIC, event);
        }
    }
}
