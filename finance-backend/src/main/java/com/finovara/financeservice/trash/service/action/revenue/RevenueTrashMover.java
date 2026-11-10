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
@Transactional
public class RevenueTrashMover {

    private final RevenueRepository revenueRepository;
    private final AutoPaymentsService autoPaymentsService;
    private final WalletService walletService;
    private final OutboxService outboxService;
    private final TrashService trashService;

    public void move(Revenue revenue, Long userId, boolean revertFinancialEffects) {
        if (revertFinancialEffects) {
            reverseRevenueFinancialTransactions(revenue, userId);
        }

        LocalDateTime deletedAt = LocalDateTime.now();
        revenue.setDeletedAt(deletedAt);
        revenueRepository.save(revenue);
        publishDeletedEvent(revenue, userId, deletedAt);
        addTrashItem(revenue, userId, deletedAt, revertFinancialEffects);
    }

    public void moveAll(List<Revenue> revenues) {
        if (revenues.isEmpty()) {
            return;
        }

        LocalDateTime deletedAt = LocalDateTime.now();
        revenues.forEach(revenue -> revenue.setDeletedAt(deletedAt));
        revenueRepository.saveAll(revenues);
        revenues.forEach(revenue -> publishDeletedEvent(revenue, revenue.getUserId(), deletedAt));
        trashService.addForAutomaticArchive(revenues.stream()
                .map(revenue -> trashItem(revenue, revenue.getUserId(), deletedAt))
                .toList());
    }

    private void reverseRevenueFinancialTransactions(Revenue revenue, Long userId) {
        autoPaymentsService.handleRevenuePiggyBankAutomation(
                userId, revenue.getAmount(), PiggyBankAutomationMode.ROLLBACK);
        walletService.removeBalanceFromWallet(
                userId, revenue.getAmount(), TransactionType.WITHDRAWAL, WalletTransactionReason.REVENUE);
    }

    private void publishDeletedEvent(Revenue revenue, Long userId, LocalDateTime deletedAt) {
        outboxService.save("Revenue", revenue.getId().toString(), "revenue.created",
                new RevenueActivityEvent(userId, RevenueActivityType.DELETED_REVENUE, revenue.getAmount(),
                        revenue.getCategory(), null, null, deletedAt));
    }

    private void addTrashItem(Revenue revenue, Long userId, LocalDateTime deletedAt,
                              boolean financialEffectsReverted) {
        if (financialEffectsReverted) {
            trashService.add(TrashEntityType.REVENUE, revenue.getId(), userId, deletedAt, null, true);
            return;
        }
        trashService.addForAutomaticArchive(List.of(trashItem(revenue, userId, deletedAt)));
    }

    private TrashItem trashItem(Revenue revenue, Long userId, LocalDateTime deletedAt) {
        return TrashItem.builder()
                .entityType(TrashEntityType.REVENUE)
                .entityId(revenue.getId())
                .userId(userId)
                .deletedAt(deletedAt)
                .build();
    }
}
