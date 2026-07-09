package com.finovara.financeservice.sharedaccount.service.deletion;

import com.finovara.contracts.event.activity.sharedaccount.SharedAccountActivityEvent;
import com.finovara.contracts.event.finance.sharedaccount.SharedAccountDeletedEvent;
import com.finovara.contracts.model.activity.SharedAccountActivityType;
import com.finovara.contracts.outbox.OutboxService;
import com.finovara.financeservice.feignclient.AuthBackendClient;
import com.finovara.financeservice.sharedaccount.repository.expense.SharedExpenseRepository;
import com.finovara.financeservice.sharedaccount.repository.revenue.SharedRevenueRepository;
import com.finovara.financeservice.sharedaccount.repository.wallet.SharedWalletRepository;
import com.finovara.financeservice.wallet.service.WalletService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SharedAccountDeletionServiceTest {

    private static final Long OWNER_ID = 1L;
    private static final Long MEMBER_ID = 2L;
    private static final String OWNER_USERNAME = "ownerUsername";
    private static final String OWNER_EMAIL = "owner@finovara.com";
    private static final String MEMBER_USERNAME = "memberUsername";
    private static final String MEMBER_EMAIL = "member@finovara.com";

    @Mock
    private WalletService walletService;

    @Mock
    private SharedExpenseRepository sharedExpenseRepository;

    @Mock
    private SharedRevenueRepository sharedRevenueRepository;

    @Mock
    private SharedWalletRepository sharedWalletRepository;

    @Mock
    private AuthBackendClient authBackendClient;

    @Mock
    private OutboxService outboxService;

    @InjectMocks
    private SharedAccountDeletionService sharedAccountDeletionService;

    private SharedAccountDeletedEvent event;

    @BeforeEach
    void setUp() {
        event = mock(SharedAccountDeletedEvent.class);
    }

    private void stubEventIds() {
        when(event.ownerId()).thenReturn(OWNER_ID);
        when(event.memberId()).thenReturn(MEMBER_ID);
    }

    private void stubNet(BigDecimal ownerRevenue, BigDecimal ownerExpense, BigDecimal memberRevenue, BigDecimal memberExpense) {
        when(sharedRevenueRepository.sumRevenueByCreatedByUserId(OWNER_ID, MEMBER_ID, OWNER_ID)).thenReturn(ownerRevenue);
        when(sharedExpenseRepository.sumExpenseByCreatedByUserId(OWNER_ID, MEMBER_ID, OWNER_ID)).thenReturn(ownerExpense);
        when(sharedRevenueRepository.sumRevenueByCreatedByUserId(OWNER_ID, MEMBER_ID, MEMBER_ID)).thenReturn(memberRevenue);
        when(sharedExpenseRepository.sumExpenseByCreatedByUserId(OWNER_ID, MEMBER_ID, MEMBER_ID)).thenReturn(memberExpense);
    }

    private void stubAuthBackendClient() {
        when(authBackendClient.getUsername(OWNER_ID)).thenReturn(OWNER_USERNAME);
        when(authBackendClient.getUserEmail(OWNER_ID)).thenReturn(OWNER_EMAIL);
        when(authBackendClient.getUsername(MEMBER_ID)).thenReturn(MEMBER_USERNAME);
        when(authBackendClient.getUserEmail(MEMBER_ID)).thenReturn(MEMBER_EMAIL);
    }

    @Nested
    class NullEventTests {

        @Test
        void shouldThrowExceptionWhenEventIsNull() {
            assertThrows(NullPointerException.class, () -> sharedAccountDeletionService.deleteData(null));
        }
    }

    @Nested
    class DeleteDataTests {

        @BeforeEach
        void setUp() {
            stubEventIds();
            stubNet(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        }

        @Test
        void shouldDeleteAllSharedDataWhenEventIsValid() {
            sharedAccountDeletionService.deleteData(event);

            verify(sharedExpenseRepository).deleteAllByOwnerIdAndMemberId(OWNER_ID, MEMBER_ID);
            verify(sharedRevenueRepository).deleteAllByOwnerIdAndMemberId(OWNER_ID, MEMBER_ID);
            verify(sharedWalletRepository).deleteByOwnerIdAndMemberId(OWNER_ID, MEMBER_ID);
        }

        @Test
        void shouldRefundBeforeDeletingDataWhenOwnerHasPositiveNet() {
            stubNet(BigDecimal.TEN, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);

            sharedAccountDeletionService.deleteData(event);

            InOrder inOrder = inOrder(walletService, sharedExpenseRepository, sharedRevenueRepository, sharedWalletRepository);
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

        @Test
        void shouldLookUpOwnerAndMemberUsernamesWhenDeletingData() {
            sharedAccountDeletionService.deleteData(event);

            verify(authBackendClient).getUsername(OWNER_ID);
            verify(authBackendClient).getUserEmail(OWNER_ID);
            verify(authBackendClient).getUsername(MEMBER_ID);
            verify(authBackendClient).getUserEmail(MEMBER_ID);
        }
    }

    @Nested
    class BothPositiveNetTests {

        @BeforeEach
        void setUp() {
            stubEventIds();
            stubNet(new BigDecimal("100"), new BigDecimal("40"), new BigDecimal("50"), new BigDecimal("10"));
            stubAuthBackendClient();
        }

        @Test
        void shouldRefundEachPartyItsOwnNetWhenBothNetsArePositive() {
            sharedAccountDeletionService.deleteData(event);

            verify(walletService).addBalanceToWallet(OWNER_ID, new BigDecimal("60"));
            verify(walletService).addBalanceToWallet(MEMBER_ID, new BigDecimal("40"));
            verifyNoMoreInteractions(walletService);
        }

        @Test
        void shouldSaveOutboxEventForOwnerWhenBothNetsArePositive() {
            ArgumentCaptor<SharedAccountActivityEvent> captor = ArgumentCaptor.forClass(SharedAccountActivityEvent.class);

            sharedAccountDeletionService.deleteData(event);

            verify(outboxService).save(eq("User"), eq(OWNER_ID.toString()), eq("activity.shared-account"), captor.capture());

            SharedAccountActivityEvent capturedEvent = captor.getValue();
            assertEquals(OWNER_ID, capturedEvent.userId());
            assertEquals(SharedAccountActivityType.REFUND_BALANCE_AFTER_LEFT_SHARED_ACCOUNT, capturedEvent.type());
            assertEquals(new BigDecimal("60"), capturedEvent.refundedBalance());
            assertEquals(OWNER_USERNAME, capturedEvent.coFounderUsername());
            assertEquals(OWNER_EMAIL, capturedEvent.coFounderEmail());
        }

        @Test
        void shouldSaveOutboxEventForMemberWhenBothNetsArePositive() {
            ArgumentCaptor<SharedAccountActivityEvent> captor = ArgumentCaptor.forClass(SharedAccountActivityEvent.class);

            sharedAccountDeletionService.deleteData(event);

            verify(outboxService).save(eq("User"), eq(MEMBER_ID.toString()), eq("activity.shared-account"), captor.capture());

            SharedAccountActivityEvent capturedEvent = captor.getValue();
            assertEquals(MEMBER_ID, capturedEvent.userId());
            assertEquals(SharedAccountActivityType.REFUND_BALANCE_AFTER_LEFT_SHARED_ACCOUNT, capturedEvent.type());
            assertEquals(new BigDecimal("40"), capturedEvent.refundedBalance());
            assertEquals(MEMBER_USERNAME, capturedEvent.coFounderUsername());
            assertEquals(MEMBER_EMAIL, capturedEvent.coFounderEmail());
        }

        @Test
        void shouldCallOutboxServiceExactlyTwiceWhenBothNetsArePositive() {
            sharedAccountDeletionService.deleteData(event);

            verify(outboxService, times(2)).save(anyString(), anyString(), anyString(), any(SharedAccountActivityEvent.class));
        }
    }

    @Nested
    class OwnerNegativeNetTests {

        @BeforeEach
        void setUp() {
            stubEventIds();
        }

        @Test
        void shouldGiveOwnerZeroAndMemberCombinedNetWhenOwnerNetIsNegativeAndCombinedIsPositive() {
            stubNet(new BigDecimal("10"), new BigDecimal("50"), new BigDecimal("100"), new BigDecimal("20"));

            sharedAccountDeletionService.deleteData(event);

            verify(walletService, never()).addBalanceToWallet(eq(OWNER_ID), any());
            verify(walletService).addBalanceToWallet(MEMBER_ID, new BigDecimal("40"));
        }

        @Test
        void shouldNotSaveOutboxEventForOwnerWhenOwnerNetIsNegativeAndCombinedIsPositive() {
            stubNet(new BigDecimal("10"), new BigDecimal("50"), new BigDecimal("100"), new BigDecimal("20"));

            sharedAccountDeletionService.deleteData(event);

            verify(outboxService, never()).save(eq("User"), eq(OWNER_ID.toString()), anyString(), any());
        }

        @Test
        void shouldGiveNoRefundWhenCombinedNetIsNonPositive() {
            stubNet(BigDecimal.ZERO, new BigDecimal("50"), new BigDecimal("10"), new BigDecimal("10"));

            sharedAccountDeletionService.deleteData(event);

            verifyNoInteractions(walletService);
        }

        @Test
        void shouldNotSaveAnyOutboxEventWhenCombinedNetIsNonPositive() {
            stubNet(BigDecimal.ZERO, new BigDecimal("50"), new BigDecimal("10"), new BigDecimal("10"));

            sharedAccountDeletionService.deleteData(event);

            verifyNoInteractions(outboxService);
        }
    }

    @Nested
    class MemberNegativeNetTests {

        @BeforeEach
        void setUp() {
            stubEventIds();
        }

        @Test
        void shouldGiveMemberZeroAndOwnerCombinedNetWhenMemberNetIsNegativeAndCombinedIsPositive() {
            stubNet(new BigDecimal("100"), new BigDecimal("20"), new BigDecimal("10"), new BigDecimal("50"));

            sharedAccountDeletionService.deleteData(event);

            verify(walletService).addBalanceToWallet(OWNER_ID, new BigDecimal("40"));
            verify(walletService, never()).addBalanceToWallet(eq(MEMBER_ID), any());
        }

        @Test
        void shouldNotSaveOutboxEventForMemberWhenMemberNetIsNegativeAndCombinedIsPositive() {
            stubNet(new BigDecimal("100"), new BigDecimal("20"), new BigDecimal("10"), new BigDecimal("50"));

            sharedAccountDeletionService.deleteData(event);

            verify(outboxService, never()).save(eq("User"), eq(MEMBER_ID.toString()), anyString(), any());
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

        @BeforeEach
        void setUp() {
            stubEventIds();
        }

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

        @Test
        void shouldDeleteSharedDataEvenWhenNoRefundOccurs() {
            stubNet(BigDecimal.ZERO, new BigDecimal("10"), BigDecimal.ZERO, new BigDecimal("5"));

            sharedAccountDeletionService.deleteData(event);

            verify(sharedExpenseRepository).deleteAllByOwnerIdAndMemberId(OWNER_ID, MEMBER_ID);
            verify(sharedRevenueRepository).deleteAllByOwnerIdAndMemberId(OWNER_ID, MEMBER_ID);
            verify(sharedWalletRepository).deleteByOwnerIdAndMemberId(OWNER_ID, MEMBER_ID);
        }
    }

    @Nested
    class ExceptionHandlingTests {

        @BeforeEach
        void setUp() {
            stubEventIds();
        }

        @Test
        void shouldThrowExceptionWhenSumRevenueByOwnerFails() {
            when(sharedRevenueRepository.sumRevenueByCreatedByUserId(OWNER_ID, MEMBER_ID, OWNER_ID))
                    .thenThrow(new RuntimeException("revenue lookup failed"));

            assertThrows(RuntimeException.class, () -> sharedAccountDeletionService.deleteData(event));
        }

        @Test
        void shouldThrowExceptionWhenSumExpenseByMemberFails() {
            stubNet(BigDecimal.TEN, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
            when(sharedExpenseRepository.sumExpenseByCreatedByUserId(OWNER_ID, MEMBER_ID, MEMBER_ID))
                    .thenThrow(new RuntimeException("expense lookup failed"));

            assertThrows(RuntimeException.class, () -> sharedAccountDeletionService.deleteData(event));
        }

        @Test
        void shouldThrowExceptionWhenAuthBackendClientFailsToGetUsername() {
            stubNet(BigDecimal.TEN, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
            when(authBackendClient.getUsername(OWNER_ID)).thenThrow(new RuntimeException("auth backend unavailable"));

            assertThrows(RuntimeException.class, () -> sharedAccountDeletionService.deleteData(event));
        }

        @Test
        void shouldThrowExceptionWhenWalletServiceFailsToAddBalance() {
            stubNet(BigDecimal.TEN, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
            doThrow(new RuntimeException("wallet update failed"))
                    .when(walletService).addBalanceToWallet(OWNER_ID, BigDecimal.TEN);

            assertThrows(RuntimeException.class, () -> sharedAccountDeletionService.deleteData(event));
        }

        @Test
        void shouldThrowExceptionWhenOutboxServiceFailsToSave() {
            stubNet(BigDecimal.TEN, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
            doThrow(new RuntimeException("outbox save failed"))
                    .when(outboxService).save(eq("User"), eq(OWNER_ID.toString()), anyString(), any());

            assertThrows(RuntimeException.class, () -> sharedAccountDeletionService.deleteData(event));
        }

        @Test
        void shouldThrowExceptionWhenSharedExpenseRepositoryDeleteFails() {
            stubNet(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
            doThrow(new RuntimeException("delete failed"))
                    .when(sharedExpenseRepository).deleteAllByOwnerIdAndMemberId(OWNER_ID, MEMBER_ID);

            assertThrows(RuntimeException.class, () -> sharedAccountDeletionService.deleteData(event));
        }

        @Test
        void shouldThrowExceptionWhenSharedRevenueRepositoryDeleteFails() {
            stubNet(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
            doThrow(new RuntimeException("delete failed"))
                    .when(sharedRevenueRepository).deleteAllByOwnerIdAndMemberId(OWNER_ID, MEMBER_ID);

            assertThrows(RuntimeException.class, () -> sharedAccountDeletionService.deleteData(event));
        }

        @Test
        void shouldThrowExceptionWhenSharedWalletRepositoryDeleteFails() {
            stubNet(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
            doThrow(new RuntimeException("delete failed"))
                    .when(sharedWalletRepository).deleteByOwnerIdAndMemberId(OWNER_ID, MEMBER_ID);

            assertThrows(RuntimeException.class, () -> sharedAccountDeletionService.deleteData(event));
        }
    }

    @Nested
    class CacheEvictionTests {

        @BeforeEach
        void setUp() {
            stubEventIds();
            stubNet(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        }

        @Test
        void shouldExecuteSuccessfullyWithCacheAnnotationsPresent() {
            sharedAccountDeletionService.deleteData(event);

            verify(sharedWalletRepository).deleteByOwnerIdAndMemberId(OWNER_ID, MEMBER_ID);
        }
    }
}