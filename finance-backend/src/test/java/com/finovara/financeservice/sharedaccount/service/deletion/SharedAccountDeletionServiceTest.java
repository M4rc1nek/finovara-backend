package com.finovara.financeservice.sharedaccount.service.deletion;

import com.finovara.contracts.event.finance.sharedaccount.SharedAccountDeletedEvent;
import com.finovara.financeservice.sharedaccount.repository.expense.SharedExpenseRepository;
import com.finovara.financeservice.sharedaccount.repository.revenue.SharedRevenueRepository;
import com.finovara.financeservice.sharedaccount.repository.wallet.SharedWalletRepository;
import com.finovara.financeservice.wallet.service.WalletService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SharedAccountDeletionServiceTest {

    private static final Long ACCOUNT_ID = 100L;
    private static final Long OWNER_ID = 1L;
    private static final Long MEMBER_ID = 2L;

    @Mock
    private WalletService walletService;

    @Mock
    private SharedExpenseRepository sharedExpenseRepository;

    @Mock
    private SharedRevenueRepository sharedRevenueRepository;

    @Mock
    private SharedWalletRepository sharedWalletRepository;

    @InjectMocks
    private SharedAccountDeletionService sharedAccountDeletionService;

    private SharedAccountDeletedEvent event;

    @BeforeEach
    void setUp() {
        event = mock(SharedAccountDeletedEvent.class);
        when(event.accountId()).thenReturn(ACCOUNT_ID);
        when(event.ownerId()).thenReturn(OWNER_ID);
        when(event.memberId()).thenReturn(MEMBER_ID);
    }

    private void stubNet(BigDecimal ownerRevenue, BigDecimal ownerExpense, BigDecimal memberRevenue, BigDecimal memberExpense) {
        when(sharedRevenueRepository.sumRevenueByCreatedByUserId(OWNER_ID, MEMBER_ID, OWNER_ID)).thenReturn(ownerRevenue);
        when(sharedExpenseRepository.sumExpenseByCreatedByUserId(OWNER_ID, MEMBER_ID, OWNER_ID)).thenReturn(ownerExpense);
        when(sharedRevenueRepository.sumRevenueByCreatedByUserId(OWNER_ID, MEMBER_ID, MEMBER_ID)).thenReturn(memberRevenue);
        when(sharedExpenseRepository.sumExpenseByCreatedByUserId(OWNER_ID, MEMBER_ID, MEMBER_ID)).thenReturn(memberExpense);
    }

    @Nested
    class DeleteDataTests {

        @BeforeEach
        void setUp() {
            stubNet(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        }

        @Test
        void shouldDeleteAllSharedData() {
            sharedAccountDeletionService.deleteData(event);

            verify(sharedExpenseRepository).deleteAllByOwnerIdAndMemberId(OWNER_ID, MEMBER_ID);
            verify(sharedRevenueRepository).deleteAllByOwnerIdAndMemberId(OWNER_ID, MEMBER_ID);
            verify(sharedWalletRepository).deleteByOwnerIdAndMemberId(OWNER_ID, MEMBER_ID);
        }

        @Test
        void shouldRefundBeforeDeletingData() {
            stubNet(BigDecimal.TEN, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);

            sharedAccountDeletionService.deleteData(event);

            var inOrder = inOrder(walletService, sharedExpenseRepository, sharedRevenueRepository, sharedWalletRepository);
            inOrder.verify(walletService).addBalanceToWallet(OWNER_ID, BigDecimal.TEN);
            inOrder.verify(sharedExpenseRepository).deleteAllByOwnerIdAndMemberId(OWNER_ID, MEMBER_ID);
            inOrder.verify(sharedRevenueRepository).deleteAllByOwnerIdAndMemberId(OWNER_ID, MEMBER_ID);
            inOrder.verify(sharedWalletRepository).deleteByOwnerIdAndMemberId(OWNER_ID, MEMBER_ID);
        }

        @Test
        void shouldNotTouchWalletServiceWhenNoRefundNeeded() {
            sharedAccountDeletionService.deleteData(event);

            verifyNoInteractions(walletService);
        }
    }

    @Nested
    class BothPositiveNetTests {

        @BeforeEach
        void setUp() {
            stubNet(new BigDecimal("100"), new BigDecimal("40"), new BigDecimal("50"), new BigDecimal("10"));
        }

        @Test
        void shouldRefundEachPartyItsOwnNet() {
            sharedAccountDeletionService.deleteData(event);

            verify(walletService).addBalanceToWallet(OWNER_ID, new BigDecimal("60"));
            verify(walletService).addBalanceToWallet(MEMBER_ID, new BigDecimal("40"));
            verifyNoMoreInteractions(walletService);
        }
    }

    @Nested
    class OwnerNegativeNetTests {

        @Test
        void shouldGiveOwnerZeroAndMemberCombinedNetWhenPositive() {
            stubNet(new BigDecimal("10"), new BigDecimal("50"), new BigDecimal("100"), new BigDecimal("20"));

            sharedAccountDeletionService.deleteData(event);

            verify(walletService, never()).addBalanceToWallet(eq(OWNER_ID), any());
            verify(walletService).addBalanceToWallet(MEMBER_ID, new BigDecimal("40"));
        }

        @Test
        void shouldGiveNoRefundWhenCombinedNetIsNonPositive() {
            stubNet(BigDecimal.ZERO, new BigDecimal("50"), new BigDecimal("10"), new BigDecimal("10"));

            sharedAccountDeletionService.deleteData(event);

            verifyNoInteractions(walletService);
        }
    }

    @Nested
    class MemberNegativeNetTests {

        @Test
        void shouldGiveMemberZeroAndOwnerCombinedNetWhenPositive() {
            stubNet(new BigDecimal("100"), new BigDecimal("20"), new BigDecimal("10"), new BigDecimal("50"));

            sharedAccountDeletionService.deleteData(event);

            verify(walletService).addBalanceToWallet(OWNER_ID, new BigDecimal("40"));
            verify(walletService, never()).addBalanceToWallet(eq(MEMBER_ID), any());
        }

        @Test
        void shouldGiveNoRefundWhenCombinedNetIsNonPositive() {
            stubNet(new BigDecimal("5"), new BigDecimal("5"), BigDecimal.ZERO, new BigDecimal("30"));

            sharedAccountDeletionService.deleteData(event);

            verifyNoInteractions(walletService);
        }
    }

    @Nested
    class EdgeCaseTests {

        @Test
        void shouldNotRefundWhenNoActivity() {
            stubNet(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);

            sharedAccountDeletionService.deleteData(event);

            verifyNoInteractions(walletService);
        }

        @Test
        void shouldNotCallWalletServiceForExactlyZeroRefund() {
            stubNet(new BigDecimal("20"), new BigDecimal("20"), new BigDecimal("30"), new BigDecimal("30"));

            sharedAccountDeletionService.deleteData(event);

            verifyNoInteractions(walletService);
        }

        @Test
        void shouldGiveNoRefundWhenBothNetsAreNegative() {
            stubNet(BigDecimal.ZERO, new BigDecimal("10"), BigDecimal.ZERO, new BigDecimal("5"));

            sharedAccountDeletionService.deleteData(event);

            verifyNoInteractions(walletService);
        }
    }

    @Nested
    class CacheEvictionTests {

        @BeforeEach
        void setUp() {
            stubNet(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        }

        @Test
        void shouldExecuteSuccessfullyWithCacheAnnotationsPresent() {
            sharedAccountDeletionService.deleteData(event);

            verify(sharedWalletRepository).deleteByOwnerIdAndMemberId(OWNER_ID, MEMBER_ID);
        }
    }
}