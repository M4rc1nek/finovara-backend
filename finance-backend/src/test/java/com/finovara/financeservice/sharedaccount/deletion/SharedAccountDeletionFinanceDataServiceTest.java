package com.finovara.financeservice.sharedaccount.deletion;

import com.finovara.contracts.event.activity.sharedaccount.SharedAccountActivityEvent;
import com.finovara.contracts.event.finance.sharedaccount.SharedAccountDeletedEvent;
import com.finovara.contracts.exception.notfound.RequestedEntityNotFoundException;
import com.finovara.contracts.outbox.OutboxService;
import com.finovara.financeservice.sharedaccount.expense.repository.SharedExpenseRepository;
import com.finovara.financeservice.sharedaccount.limit.repository.SharedLimitRepository;
import com.finovara.financeservice.sharedaccount.piggybank.repository.SharedPiggyBankRepository;
import com.finovara.financeservice.sharedaccount.revenue.model.SharedRevenueRepository;
import com.finovara.financeservice.sharedaccount.wallet.repository.SharedWalletRepository;
import com.finovara.financeservice.wallet.service.WalletService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SharedAccountDeletionFinanceDataServiceTest {

    @Mock
    private WalletService walletService;

    @Mock
    private SharedExpenseRepository sharedExpenseRepository;

    @Mock
    private SharedRevenueRepository sharedRevenueRepository;

    @Mock
    private SharedWalletRepository sharedWalletRepository;

    @Mock
    private SharedPiggyBankRepository sharedPiggyBankRepository;

    @Mock
    private SharedLimitRepository sharedLimitRepository;

    @Mock
    private OutboxService outboxService;

    @InjectMocks
    private SharedAccountDeletionFinanceDataService financeDataService;

    private Long ownerId;
    private Long memberId;

    @BeforeEach
    void setUp() {
        financeDataService = new SharedAccountDeletionFinanceDataService(
                walletService,
                sharedExpenseRepository,
                sharedRevenueRepository,
                sharedWalletRepository,
                sharedPiggyBankRepository,
                sharedLimitRepository,
                outboxService
                );
        ownerId = 1L;
        memberId = 2L;
    }

    private SharedAccountDeletedEvent buildEvent() {
        SharedAccountDeletedEvent event = mock(SharedAccountDeletedEvent.class);
        when(event.ownerId()).thenReturn(ownerId);
        when(event.memberId()).thenReturn(memberId);
        return event;
    }

    @Nested
    class WhenSharedAccountAlreadyDeleted {

        @Test
        void shouldReturnFalseWhenSharedWalletDoesNotExist() {
            SharedAccountDeletedEvent event = buildEvent();
            when(sharedWalletRepository.existsByOwnerIdAndMemberId(ownerId, memberId)).thenReturn(false);

            boolean result = financeDataService.deleteData(event);

            assertThat(result).isFalse();
        }

        @Test
        void shouldNotDeleteAnyDataWhenSharedWalletDoesNotExist() {
            SharedAccountDeletedEvent event = buildEvent();
            when(sharedWalletRepository.existsByOwnerIdAndMemberId(ownerId, memberId)).thenReturn(false);

            financeDataService.deleteData(event);

            verify(sharedExpenseRepository, never()).deleteAllByOwnerIdAndMemberId(any(), any());
            verify(sharedRevenueRepository, never()).deleteAllByOwnerIdAndMemberId(any(), any());
            verify(sharedWalletRepository, never()).deleteByOwnerIdAndMemberId(any(), any());
            verify(sharedPiggyBankRepository, never()).deleteByOwnerIdAndMemberId(any(), any());
        }

        @Test
        void shouldNotInteractWithWalletServiceWhenSharedWalletDoesNotExist() {
            SharedAccountDeletedEvent event = buildEvent();
            when(sharedWalletRepository.existsByOwnerIdAndMemberId(ownerId, memberId)).thenReturn(false);

            financeDataService.deleteData(event);

            verifyNoInteractions(walletService);
            verifyNoInteractions(outboxService);
        }
    }

    @Nested
    class WhenDeletingSharedAccountData {

        @BeforeEach
        void stubExistingAccount() {
            when(sharedWalletRepository.existsByOwnerIdAndMemberId(ownerId, memberId)).thenReturn(true);
            when(sharedRevenueRepository.sumRevenueByCreatedByUserId(ownerId, memberId, ownerId)).thenReturn(BigDecimal.ZERO);
            when(sharedExpenseRepository.sumExpenseByCreatedByUserId(ownerId, memberId, ownerId)).thenReturn(BigDecimal.ZERO);
            when(sharedRevenueRepository.sumRevenueByCreatedByUserId(ownerId, memberId, memberId)).thenReturn(BigDecimal.ZERO);
            when(sharedExpenseRepository.sumExpenseByCreatedByUserId(ownerId, memberId, memberId)).thenReturn(BigDecimal.ZERO);
        }

        @Test
        void shouldReturnTrueWhenSharedWalletExists() {
            SharedAccountDeletedEvent event = buildEvent();

            boolean result = financeDataService.deleteData(event);

            assertThat(result).isTrue();
        }

        @Test
        void shouldDeleteAllExpensesWhenSharedWalletExists() {
            SharedAccountDeletedEvent event = buildEvent();

            financeDataService.deleteData(event);

            verify(sharedExpenseRepository).deleteAllByOwnerIdAndMemberId(ownerId, memberId);
        }

        @Test
        void shouldDeleteAllRevenuesWhenSharedWalletExists() {
            SharedAccountDeletedEvent event = buildEvent();

            financeDataService.deleteData(event);

            verify(sharedRevenueRepository).deleteAllByOwnerIdAndMemberId(ownerId, memberId);
        }

        @Test
        void shouldDeleteSharedWalletWhenSharedWalletExists() {
            SharedAccountDeletedEvent event = buildEvent();

            financeDataService.deleteData(event);

            verify(sharedWalletRepository).deleteByOwnerIdAndMemberId(ownerId, memberId);
        }

        @Test
        void shouldDeletePiggyBankWhenSharedWalletExists() {
            SharedAccountDeletedEvent event = buildEvent();

            financeDataService.deleteData(event);

            verify(sharedPiggyBankRepository).deleteByOwnerIdAndMemberId(ownerId, memberId);
        }
    }

    @Nested
    class WhenCalculatingRefunds {

        @Test
        void shouldRefundBothUsersWhenBothContributionsArePositive() {
            SharedAccountDeletedEvent event = buildEvent();
            when(sharedWalletRepository.existsByOwnerIdAndMemberId(ownerId, memberId)).thenReturn(true);
            when(sharedRevenueRepository.sumRevenueByCreatedByUserId(ownerId, memberId, ownerId)).thenReturn(BigDecimal.valueOf(1000));
            when(sharedExpenseRepository.sumExpenseByCreatedByUserId(ownerId, memberId, ownerId)).thenReturn(BigDecimal.valueOf(200));
            when(sharedRevenueRepository.sumRevenueByCreatedByUserId(ownerId, memberId, memberId)).thenReturn(BigDecimal.valueOf(500));
            when(sharedExpenseRepository.sumExpenseByCreatedByUserId(ownerId, memberId, memberId)).thenReturn(BigDecimal.valueOf(100));

            financeDataService.deleteData(event);

            verify(walletService).addBalanceToWallet(ownerId, BigDecimal.valueOf(800));
            verify(walletService).addBalanceToWallet(memberId, BigDecimal.valueOf(400));
        }

        @Test
        void shouldSendActivityEventsForBothUsersWhenBothContributionsArePositive() {
            SharedAccountDeletedEvent event = buildEvent();
            when(sharedWalletRepository.existsByOwnerIdAndMemberId(ownerId, memberId)).thenReturn(true);
            when(sharedRevenueRepository.sumRevenueByCreatedByUserId(ownerId, memberId, ownerId)).thenReturn(BigDecimal.valueOf(1000));
            when(sharedExpenseRepository.sumExpenseByCreatedByUserId(ownerId, memberId, ownerId)).thenReturn(BigDecimal.valueOf(200));
            when(sharedRevenueRepository.sumRevenueByCreatedByUserId(ownerId, memberId, memberId)).thenReturn(BigDecimal.valueOf(500));
            when(sharedExpenseRepository.sumExpenseByCreatedByUserId(ownerId, memberId, memberId)).thenReturn(BigDecimal.valueOf(100));

            financeDataService.deleteData(event);

            verify(outboxService).save(eq("User"), eq(ownerId.toString()), eq("activity.shared-account"), any(SharedAccountActivityEvent.class));
            verify(outboxService).save(eq("User"), eq(memberId.toString()), eq("activity.shared-account"), any(SharedAccountActivityEvent.class));
        }

        @Test
        void shouldNotRefundOwnerWhenOwnerContributionIsNegative() {
            SharedAccountDeletedEvent event = buildEvent();
            when(sharedWalletRepository.existsByOwnerIdAndMemberId(ownerId, memberId)).thenReturn(true);
            when(sharedRevenueRepository.sumRevenueByCreatedByUserId(ownerId, memberId, ownerId)).thenReturn(BigDecimal.valueOf(100));
            when(sharedExpenseRepository.sumExpenseByCreatedByUserId(ownerId, memberId, ownerId)).thenReturn(BigDecimal.valueOf(300));
            when(sharedRevenueRepository.sumRevenueByCreatedByUserId(ownerId, memberId, memberId)).thenReturn(BigDecimal.valueOf(500));
            when(sharedExpenseRepository.sumExpenseByCreatedByUserId(ownerId, memberId, memberId)).thenReturn(BigDecimal.valueOf(100));

            financeDataService.deleteData(event);

            verify(walletService, never()).addBalanceToWallet(eq(ownerId), any());
        }

        @Test
        void shouldRefundMemberWithCombinedAmountWhenOwnerContributionIsNegative() {
            SharedAccountDeletedEvent event = buildEvent();
            when(sharedWalletRepository.existsByOwnerIdAndMemberId(ownerId, memberId)).thenReturn(true);
            when(sharedRevenueRepository.sumRevenueByCreatedByUserId(ownerId, memberId, ownerId)).thenReturn(BigDecimal.valueOf(100));
            when(sharedExpenseRepository.sumExpenseByCreatedByUserId(ownerId, memberId, ownerId)).thenReturn(BigDecimal.valueOf(300));
            when(sharedRevenueRepository.sumRevenueByCreatedByUserId(ownerId, memberId, memberId)).thenReturn(BigDecimal.valueOf(500));
            when(sharedExpenseRepository.sumExpenseByCreatedByUserId(ownerId, memberId, memberId)).thenReturn(BigDecimal.valueOf(100));

            financeDataService.deleteData(event);

            verify(walletService).addBalanceToWallet(memberId, BigDecimal.valueOf(200));
        }

        @Test
        void shouldNotRefundMemberWhenMemberContributionIsNegative() {
            SharedAccountDeletedEvent event = buildEvent();
            when(sharedWalletRepository.existsByOwnerIdAndMemberId(ownerId, memberId)).thenReturn(true);
            when(sharedRevenueRepository.sumRevenueByCreatedByUserId(ownerId, memberId, ownerId)).thenReturn(BigDecimal.valueOf(300));
            when(sharedExpenseRepository.sumExpenseByCreatedByUserId(ownerId, memberId, ownerId)).thenReturn(BigDecimal.ZERO);
            when(sharedRevenueRepository.sumRevenueByCreatedByUserId(ownerId, memberId, memberId)).thenReturn(BigDecimal.valueOf(200));
            when(sharedExpenseRepository.sumExpenseByCreatedByUserId(ownerId, memberId, memberId)).thenReturn(BigDecimal.valueOf(300));

            financeDataService.deleteData(event);

            verify(walletService, never()).addBalanceToWallet(eq(memberId), any());
        }

        @Test
        void shouldRefundOwnerWithCombinedAmountWhenMemberContributionIsNegative() {
            SharedAccountDeletedEvent event = buildEvent();
            when(sharedWalletRepository.existsByOwnerIdAndMemberId(ownerId, memberId)).thenReturn(true);
            when(sharedRevenueRepository.sumRevenueByCreatedByUserId(ownerId, memberId, ownerId)).thenReturn(BigDecimal.valueOf(300));
            when(sharedExpenseRepository.sumExpenseByCreatedByUserId(ownerId, memberId, ownerId)).thenReturn(BigDecimal.ZERO);
            when(sharedRevenueRepository.sumRevenueByCreatedByUserId(ownerId, memberId, memberId)).thenReturn(BigDecimal.valueOf(200));
            when(sharedExpenseRepository.sumExpenseByCreatedByUserId(ownerId, memberId, memberId)).thenReturn(BigDecimal.valueOf(300));

            financeDataService.deleteData(event);

            verify(walletService).addBalanceToWallet(ownerId, BigDecimal.valueOf(200));
        }

        @Test
        void shouldNotRefundAnyoneWhenCombinedNetContributionIsNegative() {
            SharedAccountDeletedEvent event = buildEvent();
            when(sharedWalletRepository.existsByOwnerIdAndMemberId(ownerId, memberId)).thenReturn(true);
            when(sharedRevenueRepository.sumRevenueByCreatedByUserId(ownerId, memberId, ownerId)).thenReturn(BigDecimal.ZERO);
            when(sharedExpenseRepository.sumExpenseByCreatedByUserId(ownerId, memberId, ownerId)).thenReturn(BigDecimal.valueOf(500));
            when(sharedRevenueRepository.sumRevenueByCreatedByUserId(ownerId, memberId, memberId)).thenReturn(BigDecimal.valueOf(100));
            when(sharedExpenseRepository.sumExpenseByCreatedByUserId(ownerId, memberId, memberId)).thenReturn(BigDecimal.ZERO);

            financeDataService.deleteData(event);

            verifyNoInteractions(walletService);
        }

        @Test
        void shouldSkipRefundWhenNetContributionIsZero() {
            SharedAccountDeletedEvent event = buildEvent();
            when(sharedWalletRepository.existsByOwnerIdAndMemberId(ownerId, memberId)).thenReturn(true);
            when(sharedRevenueRepository.sumRevenueByCreatedByUserId(ownerId, memberId, ownerId)).thenReturn(BigDecimal.valueOf(100));
            when(sharedExpenseRepository.sumExpenseByCreatedByUserId(ownerId, memberId, ownerId)).thenReturn(BigDecimal.valueOf(100));
            when(sharedRevenueRepository.sumRevenueByCreatedByUserId(ownerId, memberId, memberId)).thenReturn(BigDecimal.ZERO);
            when(sharedExpenseRepository.sumExpenseByCreatedByUserId(ownerId, memberId, memberId)).thenReturn(BigDecimal.ZERO);

            financeDataService.deleteData(event);

            verify(walletService, never()).addBalanceToWallet(eq(ownerId), any());
        }
    }

    @Nested
    class WhenWalletServiceThrowsException {

        @BeforeEach
        void stubExistingAccountWithPositiveContributions() {
            when(sharedWalletRepository.existsByOwnerIdAndMemberId(ownerId, memberId)).thenReturn(true);
            when(sharedRevenueRepository.sumRevenueByCreatedByUserId(ownerId, memberId, ownerId)).thenReturn(BigDecimal.valueOf(1000));
            when(sharedExpenseRepository.sumExpenseByCreatedByUserId(ownerId, memberId, ownerId)).thenReturn(BigDecimal.valueOf(200));
            when(sharedRevenueRepository.sumRevenueByCreatedByUserId(ownerId, memberId, memberId)).thenReturn(BigDecimal.valueOf(500));
            when(sharedExpenseRepository.sumExpenseByCreatedByUserId(ownerId, memberId, memberId)).thenReturn(BigDecimal.valueOf(100));
        }

        @Test
        void shouldSkipOutboxEventWhenOwnerWalletDoesNotExist() {
            SharedAccountDeletedEvent event = buildEvent();
            doThrow(new RequestedEntityNotFoundException("wallet not found"))
                    .when(walletService).addBalanceToWallet(eq(ownerId), any());

            financeDataService.deleteData(event);

            verify(outboxService, never()).save(eq("User"), eq(ownerId.toString()), any(), any());
        }

        @Test
        void shouldStillRefundMemberWhenOwnerWalletDoesNotExist() {
            SharedAccountDeletedEvent event = buildEvent();
            doThrow(new RequestedEntityNotFoundException("wallet not found"))
                    .when(walletService).addBalanceToWallet(eq(ownerId), any());

            financeDataService.deleteData(event);

            verify(walletService).addBalanceToWallet(memberId, BigDecimal.valueOf(400));
        }

        @Test
        void shouldStillDeleteSharedDataWhenWalletServiceThrowsException() {
            SharedAccountDeletedEvent event = buildEvent();
            doThrow(new RequestedEntityNotFoundException("wallet not found"))
                    .when(walletService).addBalanceToWallet(eq(ownerId), any());

            boolean result = financeDataService.deleteData(event);

            assertThat(result).isTrue();
            verify(sharedExpenseRepository).deleteAllByOwnerIdAndMemberId(ownerId, memberId);
        }
    }

    @Nested
    class WhenInputIsInvalid {

        @Test
        void shouldThrowExceptionWhenOwnerIdIsNull() {
            SharedAccountDeletedEvent event = mock(SharedAccountDeletedEvent.class);
            when(event.ownerId()).thenReturn(null);

            assertThrows(NullPointerException.class, () -> financeDataService.deleteData(event));
        }

        @Test
        void shouldThrowExceptionWhenMemberIdIsNull() {
            SharedAccountDeletedEvent event = mock(SharedAccountDeletedEvent.class);
            when(event.ownerId()).thenReturn(ownerId);
            when(event.memberId()).thenReturn(null);

            assertThrows(NullPointerException.class, () -> financeDataService.deleteData(event));
        }

        @Test
        void shouldNotInteractWithRepositoriesWhenOwnerIdIsNull() {
            SharedAccountDeletedEvent event = mock(SharedAccountDeletedEvent.class);
            when(event.ownerId()).thenReturn(null);

            assertThrows(NullPointerException.class, () -> financeDataService.deleteData(event));

            verifyNoInteractions(sharedWalletRepository);
        }
    }
}