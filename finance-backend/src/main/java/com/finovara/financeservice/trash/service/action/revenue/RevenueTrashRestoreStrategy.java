package com.finovara.financeservice.trash.service.action.revenue;

import com.finovara.contracts.exception.notfound.RequestedEntityNotFoundException;
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
import com.finovara.financeservice.trash.service.TrashRestoreStrategy;
import com.finovara.financeservice.wallet.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class RevenueTrashRestoreStrategy implements TrashRestoreStrategy {

    private final RevenueRepository revenueRepository;
    private final WalletService walletService;
    private final AutoPaymentsService autoPaymentsService;
    private final OutboxService outboxService;

    @Override
    public boolean supports(TrashEntityType entityType) {
        return entityType == TrashEntityType.REVENUE;
    }

    @Override
    public void restore(TrashItem item) {
        Revenue revenue = getRevenue(item);
        restoreRevenue(revenue);
        restoreFinancialEffects(item, revenue);
        publishRevenueCreatedEvent(item, revenue);
    }

    private Revenue getRevenue(TrashItem item) {
        return revenueRepository.findTrashedByIdAndUserIdForUpdate(item.getEntityId(), item.getUserId())
                .orElseThrow(() -> new RequestedEntityNotFoundException("Revenue not found in trash"));
    }

    private void restoreRevenue(Revenue revenue) {
        revenue.setDeletedAt(null);
        revenueRepository.save(revenue);
    }

    private void restoreFinancialEffects(TrashItem item, Revenue revenue) {
        if (!Boolean.TRUE.equals(item.getFinancialEffectsReverted())) {
            return;
        }
        walletService.addBalanceToWallet(item.getUserId(), revenue.getAmount(), TransactionType.DEPOSIT, WalletTransactionReason.REVENUE);
        autoPaymentsService.handleRevenuePiggyBankAutomation(item.getUserId(), revenue.getAmount(), PiggyBankAutomationMode.APPLY);
    }

    private void publishRevenueCreatedEvent(TrashItem item, Revenue revenue) {
        outboxService.save("Revenue", revenue.getId().toString(), "revenue.created",
                new RevenueActivityEvent(item.getUserId(), RevenueActivityType.ADDED_REVENUE, revenue.getAmount(),
                        revenue.getCategory(), null, null, LocalDateTime.now()));
    }
}
