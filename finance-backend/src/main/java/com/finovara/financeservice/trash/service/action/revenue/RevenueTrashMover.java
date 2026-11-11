package com.finovara.financeservice.trash.service.action.revenue;

import com.finovara.contracts.mainaccount.activity.event.revenue.RevenueActivityEvent;
import com.finovara.contracts.mainaccount.activity.model.RevenueActivityType;
import com.finovara.contracts.mainaccount.activity.model.TransactionType;
import com.finovara.contracts.mainaccount.activity.model.WalletTransactionReason;
import com.finovara.contracts.outbox.OutboxService;
import com.finovara.financeservice.revenue.model.Revenue;
import com.finovara.financeservice.revenue.repository.RevenueRepository;
import com.finovara.financeservice.settings.piggybank.autopayments.model.PiggyBankAutomationMode;
import com.finovara.financeservice.settings.piggybank.autopayments.service.AutoPaymentsService;
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
public class RevenueTrashMover {

    private final RevenueRepository revenueRepository;
    private final AutoPaymentsService autoPaymentsService;
    private final WalletService walletService;
    private final OutboxService outboxService;
    private final TrashService trashService;

    @Transactional
    public void move(Revenue revenue, Long userId, boolean revertFinancialEffects) {
        if (revertFinancialEffects) {
            reverseRevenueFinancialTransactions(revenue, userId);
        }

        LocalDateTime deletedAt = LocalDateTime.now();
        revenue.setDeletedAt(deletedAt);
        revenueRepository.save(revenue);
        publishAddedToTrashEvent(revenue, userId, deletedAt);
        trashService.add(TrashEntityType.REVENUE, revenue.getId(), userId, deletedAt, null, revertFinancialEffects);
    }

    @Transactional
    public void moveAll(List<Revenue> revenues) {
        if (revenues.isEmpty()) {
            return;
        }

        LocalDateTime deletedAt = LocalDateTime.now();
        revenues.forEach(revenue -> revenue.setDeletedAt(deletedAt));
        revenueRepository.saveAll(revenues);
        revenues.forEach(revenue -> publishAddedToTrashEvent(revenue, revenue.getUserId(), deletedAt));
        trashService.addAll(revenues.stream()
                .map(revenue -> trashItem(revenue, deletedAt))
                .toList());
    }

    private void reverseRevenueFinancialTransactions(Revenue revenue, Long userId) {
        autoPaymentsService.handleRevenuePiggyBankAutomation(userId, revenue.getAmount(), PiggyBankAutomationMode.ROLLBACK);
        walletService.removeBalanceFromWallet(userId, revenue.getAmount(), TransactionType.WITHDRAWAL, WalletTransactionReason.REVENUE);
    }

    private void publishAddedToTrashEvent(Revenue revenue, Long userId, LocalDateTime deletedAt) {
        outboxService.save("Revenue", revenue.getId().toString(), "trash.revenue.activity",
                new RevenueActivityEvent(userId, RevenueActivityType.ADDED_TO_TRASH, revenue.getAmount(),
                        revenue.getCategory(), null, null, deletedAt));
    }

    private TrashItem trashItem(Revenue revenue, LocalDateTime deletedAt) {
        return TrashItem.builder()
                .entityType(TrashEntityType.REVENUE)
                .entityId(revenue.getId())
                .userId(revenue.getUserId())
                .deletedAt(deletedAt)
                .financialEffectsReverted(false)
                .build();
    }
}