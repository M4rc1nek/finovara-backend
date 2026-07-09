package com.finovara.financeservice.sharedaccount.service.deletion;

import com.finovara.contracts.event.finance.sharedaccount.SharedAccountDeletedEvent;
import com.finovara.contracts.exception.notfound.RequestedEntityNotFoundException;
import com.finovara.contracts.outbox.OutboxService;
import com.finovara.financeservice.sharedaccount.repository.expense.SharedExpenseRepository;
import com.finovara.financeservice.sharedaccount.repository.revenue.SharedRevenueRepository;
import com.finovara.financeservice.sharedaccount.repository.wallet.SharedWalletRepository;
import com.finovara.financeservice.wallet.service.WalletService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SharedAccountDeletionFinanceDataServiceTest {

    private static final Long OWNER_ID = 2L;
    private static final Long MEMBER_ID = 1L;
    private static final Long ACCOUNT_ID = 99L;

    @Mock
    private WalletService walletService;
    @Mock
    private SharedExpenseRepository sharedExpenseRepository;
    @Mock
    private SharedRevenueRepository sharedRevenueRepository;
    @Mock
    private SharedWalletRepository sharedWalletRepository;
    @Mock
    private OutboxService outboxService;
    @Mock
    private CacheManager cacheManager;
    @Mock
    private Cache cache;

    private SharedAccountDeletionFinanceDataService service;

    @BeforeEach
    void setUp() {
        service = new SharedAccountDeletionFinanceDataService(
                walletService, sharedExpenseRepository, sharedRevenueRepository,
                sharedWalletRepository, outboxService, cacheManager);
    }

    private SharedAccountDeletedEvent event(Long ownerId, Long memberId) {
        return new SharedAccountDeletedEvent(ACCOUNT_ID, ownerId, memberId, memberId,
                "ownerUsername", "owner@test.com", "memberUsername", "member@test.com");
    }

    private void stubNoContributions() {
        when(sharedRevenueRepository.sumRevenueByCreatedByUserId(any(), any(), any())).thenReturn(BigDecimal.ZERO);
        when(sharedExpenseRepository.sumExpenseByCreatedByUserId(any(), any(), any())).thenReturn(BigDecimal.ZERO);
    }

    private void stubContribution(Long contributorId, BigDecimal revenue, BigDecimal expense) {
        when(sharedRevenueRepository.sumRevenueByCreatedByUserId(OWNER_ID, MEMBER_ID, contributorId)).thenReturn(revenue);
        when(sharedExpenseRepository.sumExpenseByCreatedByUserId(OWNER_ID, MEMBER_ID, contributorId)).thenReturn(expense);
    }

    @Nested
    class WhenEventHasMissingRequiredData {

        @Test
        void missingOwnerIdThrows() {
            SharedAccountDeletedEvent invalidEvent = event(null, MEMBER_ID);

            assertThrows(NullPointerException.class, () -> service.deleteData(invalidEvent));

            verifyNoInteractions(walletService, outboxService);
        }

        @Test
        void missingMemberIdThrows() {
            SharedAccountDeletedEvent invalidEvent = event(OWNER_ID, null);

            assertThrows(NullPointerException.class, () -> service.deleteData(invalidEvent));

            verifyNoInteractions(walletService, outboxService);
        }
    }

    @Nested
    class WhenDeletionAlreadyProcessed {

        private SharedAccountDeletedEvent duplicateEvent;

        @BeforeEach
        void alreadyDeleted() {
            duplicateEvent = event(OWNER_ID, MEMBER_ID);
            when(sharedWalletRepository.existsByOwnerIdAndMemberId(OWNER_ID, MEMBER_ID)).thenReturn(false);
        }

        @Test
        void skipsProcessingWithoutSideEffects() {
            assertDoesNotThrow(() -> service.deleteData(duplicateEvent));

            verifyNoInteractions(walletService, outboxService);
            verify(sharedExpenseRepository, never()).deleteAllByOwnerIdAndMemberId(any(), any());
            verify(sharedRevenueRepository, never()).deleteAllByOwnerIdAndMemberId(any(), any());
            verify(sharedWalletRepository, never()).deleteByOwnerIdAndMemberId(any(), any());
        }
    }

    @Nested
    class WhenProcessingFreshDeletion {

        private SharedAccountDeletedEvent freshEvent;

        @BeforeEach
        void notYetProcessed() {
            freshEvent = event(OWNER_ID, MEMBER_ID);
            when(sharedWalletRepository.existsByOwnerIdAndMemberId(OWNER_ID, MEMBER_ID)).thenReturn(true);
        }

        @Test
        void deletesSharedFinancialData() {
            stubNoContributions();

            service.deleteData(freshEvent);

            verify(sharedExpenseRepository).deleteAllByOwnerIdAndMemberId(OWNER_ID, MEMBER_ID);
            verify(sharedRevenueRepository).deleteAllByOwnerIdAndMemberId(OWNER_ID, MEMBER_ID);
            verify(sharedWalletRepository).deleteByOwnerIdAndMemberId(OWNER_ID, MEMBER_ID);
        }

        @Test
        void bothPositiveNet_bothRefunded() {
            stubContribution(OWNER_ID, new BigDecimal("500"), new BigDecimal("100"));
            stubContribution(MEMBER_ID, new BigDecimal("300"), new BigDecimal("50"));

            service.deleteData(freshEvent);

            verify(walletService).addBalanceToWallet(OWNER_ID, new BigDecimal("400"));
            verify(walletService).addBalanceToWallet(MEMBER_ID, new BigDecimal("250"));
            verify(outboxService, times(2)).save(eq("User"), any(), eq("activity.shared-account"), any());
        }

        @Test
        void ownerNegativeNet_onlyMemberRefundedWithReducedAmount() {
            stubContribution(OWNER_ID, BigDecimal.ZERO, new BigDecimal("200"));
            stubContribution(MEMBER_ID, new BigDecimal("300"), BigDecimal.ZERO);

            service.deleteData(freshEvent);

            verify(walletService, never()).addBalanceToWallet(eq(OWNER_ID), any());
            verify(walletService).addBalanceToWallet(MEMBER_ID, new BigDecimal("100"));
            verify(outboxService, times(1)).save(eq("User"), eq(MEMBER_ID.toString()), eq("activity.shared-account"), any());
        }

        @Test
        void netContributionsCancelOut_noRefundIssued() {
            stubContribution(OWNER_ID, BigDecimal.ZERO, new BigDecimal("300"));
            stubContribution(MEMBER_ID, new BigDecimal("300"), BigDecimal.ZERO);

            service.deleteData(freshEvent);

            verifyNoInteractions(walletService, outboxService);
        }

        @Test
        void evictsWalletCachesForBothUsers() {
            stubNoContributions();
            when(cacheManager.getCache(any())).thenReturn(cache);

            service.deleteData(freshEvent);

            verify(cacheManager, times(2)).getCache("wallet:shared");
            verify(cacheManager, times(2)).getCache("wallet:user");
            verify(cache, times(2)).evict(OWNER_ID);
            verify(cache, times(2)).evict(MEMBER_ID);
        }
    }

    @Nested
    class WhenWalletIsMissingDuringRefund {

        private SharedAccountDeletedEvent freshEvent;

        @BeforeEach
        void ownerHasPositiveNetButWalletIsGone() {
            freshEvent = event(OWNER_ID, MEMBER_ID);
            when(sharedWalletRepository.existsByOwnerIdAndMemberId(OWNER_ID, MEMBER_ID)).thenReturn(true);

            stubContribution(OWNER_ID, new BigDecimal("400"), BigDecimal.ZERO);
            stubContribution(MEMBER_ID, BigDecimal.ZERO, BigDecimal.ZERO);

            when(walletService.addBalanceToWallet(eq(OWNER_ID), any()))
                    .thenThrow(new RequestedEntityNotFoundException("Wallet not found for this user"));
        }

        @Test
        void doesNotPropagateException_andStillDeletesSharedData() {
            assertDoesNotThrow(() -> service.deleteData(freshEvent));

            verify(sharedWalletRepository).deleteByOwnerIdAndMemberId(OWNER_ID, MEMBER_ID);
        }

        @Test
        void skipsActivityOutboxEventForMissingWallet() {
            service.deleteData(freshEvent);

            verify(outboxService, never()).save(eq("User"), eq(OWNER_ID.toString()), eq("activity.shared-account"), any());
        }
    }
}