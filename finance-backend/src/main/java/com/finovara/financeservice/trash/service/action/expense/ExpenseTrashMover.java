package com.finovara.financeservice.trash.service.action.expense;

import com.finovara.contracts.mainaccount.activity.event.expense.ExpenseActivityEvent;
import com.finovara.contracts.mainaccount.activity.model.ExpenseActivityType;
import com.finovara.contracts.mainaccount.activity.model.TransactionType;
import com.finovara.contracts.mainaccount.activity.model.WalletTransactionReason;
import com.finovara.contracts.outbox.OutboxService;
import com.finovara.financeservice.expense.model.Expense;
import com.finovara.financeservice.expense.repository.ExpenseRepository;
import com.finovara.financeservice.settings.piggybank.autopayments.model.PiggyBankAutomationMode;
import com.finovara.financeservice.settings.piggybank.roundup.service.RoundUpService;
import com.finovara.financeservice.trash.model.TrashEntityType;
import com.finovara.financeservice.trash.model.TrashItem;
import com.finovara.financeservice.trash.service.TrashService;
import com.finovara.financeservice.wallet.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Transactional
public class ExpenseTrashMover {

    private final ExpenseRepository expenseRepository;
    private final RoundUpService roundUpService;
    private final WalletService walletService;
    private final OutboxService outboxService;
    private final TrashService trashService;

    public void move(Expense expense, Long userId, boolean revertFinancialEffects) {
        if (revertFinancialEffects) {
            revertFinancialEffects(expense, userId);
        }

        LocalDateTime deletedAt = LocalDateTime.now();
        expense.setDeletedAt(deletedAt);
        expenseRepository.save(expense);
        publishDeletedEvent(expense, userId, deletedAt);
        addTrashItem(expense, userId, deletedAt, revertFinancialEffects);
    }

    public void moveAll(List<Expense> expenses) {
        if (expenses.isEmpty()) {
            return;
        }

        LocalDateTime deletedAt = LocalDateTime.now();
        expenses.forEach(expense -> expense.setDeletedAt(deletedAt));
        expenseRepository.saveAll(expenses);
        expenses.forEach(expense -> publishDeletedEvent(expense, expense.getUserId(), deletedAt));
        trashService.addForAutomaticArchive(expenses.stream()
                .map(expense -> trashItem(expense, expense.getUserId(), deletedAt))
                .toList());
    }

    private void revertFinancialEffects(Expense expense, Long userId) {
        roundUpService.handleExpenseForRoundUp(userId, expense.getId(), PiggyBankAutomationMode.ROLLBACK);
        walletService.addBalanceToWallet(userId, expense.getAmount(),
                TransactionType.REFUND, WalletTransactionReason.EXPENSE);
    }

    private void publishDeletedEvent(Expense expense, Long userId, LocalDateTime deletedAt) {
        outboxService.save("Expense", expense.getId().toString(), "expense.created",
                new ExpenseActivityEvent(userId, ExpenseActivityType.DELETED_EXPENSE, expense.getAmount(),
                        expense.getCategory(), null, null, deletedAt));
    }

    private void addTrashItem(Expense expense, Long userId, LocalDateTime deletedAt,
                              boolean financialEffectsReverted) {
        if (financialEffectsReverted) {
            trashService.add(TrashEntityType.EXPENSE, expense.getId(), userId, deletedAt, null, true);
            return;
        }
        trashService.addForAutomaticArchive(List.of(trashItem(expense, userId, deletedAt)));
    }

    private TrashItem trashItem(Expense expense, Long userId, LocalDateTime deletedAt) {
        return TrashItem.builder()
                .entityType(TrashEntityType.EXPENSE)
                .entityId(expense.getId())
                .userId(userId)
                .deletedAt(deletedAt)
                .build();
    }
}
