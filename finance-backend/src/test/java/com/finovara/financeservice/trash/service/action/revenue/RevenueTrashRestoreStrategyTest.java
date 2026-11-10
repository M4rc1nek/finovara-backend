package com.finovara.financeservice.trash.service.action.revenue;

import com.finovara.contracts.exception.notfound.RequestedEntityNotFoundException;
import com.finovara.contracts.outbox.OutboxService;
import com.finovara.contracts.util.model.RevenueCategory;
import com.finovara.financeservice.revenue.model.Revenue;
import com.finovara.financeservice.revenue.repository.RevenueRepository;
import com.finovara.financeservice.settings.piggybank.autopayments.service.AutoPaymentsService;
import com.finovara.financeservice.trash.model.TrashEntityType;
import com.finovara.financeservice.trash.model.TrashItem;
import com.finovara.financeservice.wallet.service.WalletService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static com.finovara.contracts.mainaccount.activity.model.TransactionType.DEPOSIT;
import static com.finovara.contracts.mainaccount.activity.model.WalletTransactionReason.REVENUE;
import static com.finovara.financeservice.settings.piggybank.autopayments.model.PiggyBankAutomationMode.APPLY;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RevenueTrashRestoreStrategyTest {

    private static final Long REVENUE_ID = 10L;
    private static final Long USER_ID = 2L;
    private static final BigDecimal AMOUNT = BigDecimal.TEN;

    @Mock
    private RevenueRepository revenueRepository;
    @Mock
    private WalletService walletService;
    @Mock
    private AutoPaymentsService autoPaymentsService;
    @Mock
    private OutboxService outboxService;

    @InjectMocks
    private RevenueTrashRestoreStrategy strategy;

    private Revenue revenue;
    private TrashItem item;

    @BeforeEach
    void setUp() {
        revenue = Revenue.builder()
                .id(REVENUE_ID)
                .userId(USER_ID)
                .amount(AMOUNT)
                .category(RevenueCategory.values()[0])
                .build();
        item = TrashItem.builder()
                .entityType(TrashEntityType.REVENUE)
                .entityId(REVENUE_ID)
                .userId(USER_ID)
                .build();
        when(revenueRepository.findTrashedByIdAndUserIdForUpdate(REVENUE_ID, USER_ID))
                .thenReturn(Optional.of(revenue));
    }

    @Nested
    class Restore {
        @Test
        void shouldRestoreFinancialEffectsWhenFlagIsTrue() {
            item.setFinancialEffectsReverted(true);

            strategy.restore(item);

            verify(revenueRepository).save(revenue);
            verify(walletService).addBalanceToWallet(USER_ID, AMOUNT, DEPOSIT, REVENUE);
            verify(autoPaymentsService).handleRevenuePiggyBankAutomation(USER_ID, AMOUNT, APPLY);
        }

        @Test
        void shouldNotRestoreFinancialEffectsWhenFlagIsFalse() {
            item.setFinancialEffectsReverted(false);

            strategy.restore(item);

            verify(revenueRepository).save(revenue);
            verifyNoInteractions(walletService, autoPaymentsService);
        }

        @Test
        void shouldNotRestoreFinancialEffectsWhenFlagIsNull() {
            strategy.restore(item);

            verify(revenueRepository).save(revenue);
            verifyNoInteractions(walletService, autoPaymentsService);
        }

        @Test
        void shouldThrowExceptionWhenRevenueIsMissingFromTrash() {
            when(revenueRepository.findTrashedByIdAndUserIdForUpdate(REVENUE_ID, USER_ID))
                    .thenReturn(Optional.empty());

            assertThrows(RequestedEntityNotFoundException.class, () -> strategy.restore(item));
        }
    }
}
