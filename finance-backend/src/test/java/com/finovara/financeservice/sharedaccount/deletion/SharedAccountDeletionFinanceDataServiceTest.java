package com.finovara.financeservice.sharedaccount.deletion;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.finovara.contracts.exception.notfound.RequestedEntityNotFoundException;
import com.finovara.contracts.mainaccount.activity.event.sharedaccount.SharedAccountChangeHistoryActivityEvent;
import com.finovara.contracts.outbox.OutboxService;
import com.finovara.contracts.sharedaccount.event.deletion.SharedAccountDeletedEvent;
import com.finovara.financeservice.sharedaccount.expense.repository.SharedExpenseRepository;
import com.finovara.financeservice.sharedaccount.revenue.repository.SharedRevenueRepository;
import com.finovara.financeservice.sharedaccount.wallet.repository.SharedWalletRepository;
import com.finovara.financeservice.wallet.service.WalletService;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SharedAccountDeletionFinanceDataServiceTest {

    private static final Long OWNER_ID = 1L;
    private static final Long MEMBER_ID = 2L;
    private static final String OUTBOX_AGGREGATE = "User";
    private static final String OUTBOX_EVENT_TYPE = "shared-account.changed";

    @Mock
    private WalletService walletService;
    @Mock
    private SharedExpenseRepository sharedExpenseRepository;
    @Mock
    private SharedRevenueRepository sharedRevenueRepository;
    @Mock
    private SharedWalletRepository sharedWalletRepository;
    @Mock
    private SharedAccountFinanceDeleter sharedAccountFinanceDeleter;
    @Mock
    private OutboxService outboxService;
    @Mock
    private SharedAccountDeletedEvent event;

    @InjectMocks
    private SharedAccountDeletionFinanceDataService service;

    private void stubContributions(String ownerRevenue, String ownerExpense, String memberRevenue, String memberExpense) {
        when(sharedRevenueRepository.sumRevenueByCreatedByUserId(OWNER_ID, MEMBER_ID, OWNER_ID)).thenReturn(new BigDecimal(ownerRevenue));
        when(sharedExpenseRepository.sumExpenseByCreatedByUserId(OWNER_ID, MEMBER_ID, OWNER_ID)).thenReturn(new BigDecimal(ownerExpense));
        when(sharedRevenueRepository.sumRevenueByCreatedByUserId(OWNER_ID, MEMBER_ID, MEMBER_ID)).thenReturn(new BigDecimal(memberRevenue));
        when(sharedExpenseRepository.sumExpenseByCreatedByUserId(OWNER_ID, MEMBER_ID, MEMBER_ID)).thenReturn(new BigDecimal(memberExpense));
    }

        @Nested
        class WhenEventIsValid {

            @BeforeEach
            void setUp() {
                when(event.ownerId()).thenReturn(OWNER_ID);
                when(event.memberId()).thenReturn(MEMBER_ID);
            }

            @Test
            void shouldReturnFalseWhenDataWasAlreadyDeleted() {
                when(sharedWalletRepository.existsByOwnerIdAndMemberId(OWNER_ID, MEMBER_ID)).thenReturn(false);

                boolean result = service.deleteData(event);

                assertFalse(result);
            }

            @Test
            void shouldSkipRefundAndDeletionWhenDataWasAlreadyDeleted() {
                when(sharedWalletRepository.existsByOwnerIdAndMemberId(OWNER_ID, MEMBER_ID)).thenReturn(false);

                service.deleteData(event);

                verifyNoInteractions(walletService, outboxService, sharedAccountFinanceDeleter, sharedRevenueRepository, sharedExpenseRepository);
            }

            @Nested
            class WhenSharedWalletExists {

                @BeforeEach
                void setUp() {
                    when(sharedWalletRepository.existsByOwnerIdAndMemberId(OWNER_ID, MEMBER_ID)).thenReturn(true);
                }

                @Test
                void shouldReturnTrueWhenDataIsDeleted() {
                    stubContributions("0.00", "0.00", "0.00", "0.00");

                    boolean result = service.deleteData(event);

                    assertTrue(result);
                }

                @Test
                void shouldDeleteAllSharedDataWhenDataIsDeleted() {
                    stubContributions("0.00", "0.00", "0.00", "0.00");

                    service.deleteData(event);

                    verify(sharedAccountFinanceDeleter).deleteAll(OWNER_ID, MEMBER_ID);
                }

                @Test
                void shouldRefundBothUsersWhenBothContributionsArePositive() {
                    stubContributions("150.00", "50.00", "70.00", "20.00");

                    service.deleteData(event);

                    verify(walletService).addBalanceToWallet(OWNER_ID, new BigDecimal("100.00"));
                    verify(walletService).addBalanceToWallet(MEMBER_ID, new BigDecimal("50.00"));
                }

                @Test
                void shouldPublishHistoryEventForBothUsersWhenBothAreRefunded() {
                    stubContributions("150.00", "50.00", "70.00", "20.00");

                    service.deleteData(event);

                    verify(outboxService).save(eq(OUTBOX_AGGREGATE), eq(OWNER_ID.toString()), eq(OUTBOX_EVENT_TYPE),
                            any(SharedAccountChangeHistoryActivityEvent.class));
                    verify(outboxService).save(eq(OUTBOX_AGGREGATE), eq(MEMBER_ID.toString()), eq(OUTBOX_EVENT_TYPE),
                            any(SharedAccountChangeHistoryActivityEvent.class));
                }

                @Test
                void shouldRefundOnlyOwnerWhenMemberDidNotContribute() {
                    stubContributions("100.00", "0.00", "0.00", "0.00");

                    service.deleteData(event);

                    verify(walletService).addBalanceToWallet(OWNER_ID, new BigDecimal("100.00"));
                    verify(walletService, never()).addBalanceToWallet(eq(MEMBER_ID), any(BigDecimal.class));
                }

                @Test
                void shouldReduceMemberRefundByOwnerDeficitWhenOwnerContributionIsNegative() {
                    stubContributions("10.00", "40.00", "80.00", "30.00");

                    service.deleteData(event);

                    verify(walletService).addBalanceToWallet(MEMBER_ID, new BigDecimal("20.00"));
                    verify(walletService, never()).addBalanceToWallet(eq(OWNER_ID), any(BigDecimal.class));
                }

                @Test
                void shouldReduceOwnerRefundByMemberDeficitWhenMemberContributionIsNegative() {
                    stubContributions("80.00", "30.00", "10.00", "40.00");

                    service.deleteData(event);

                    verify(walletService).addBalanceToWallet(OWNER_ID, new BigDecimal("20.00"));
                    verify(walletService, never()).addBalanceToWallet(eq(MEMBER_ID), any(BigDecimal.class));
                }

                @Test
                void shouldNotRefundAnyoneWhenOwnerDeficitExceedsMemberContribution() {
                    stubContributions("10.00", "60.00", "40.00", "20.00");

                    service.deleteData(event);

                    verifyNoInteractions(walletService, outboxService);
                }

                @Test
                void shouldNotRefundAnyoneWhenMemberDeficitExceedsOwnerContribution() {
                    stubContributions("40.00", "20.00", "10.00", "60.00");

                    service.deleteData(event);

                    verifyNoInteractions(walletService, outboxService);
                }

                @Test
                void shouldNotRefundAnyoneWhenBothContributionsAreNegative() {
                    stubContributions("10.00", "40.00", "5.00", "25.00");

                    service.deleteData(event);

                    verifyNoInteractions(walletService, outboxService);
                }

                @Test
                void shouldNotRefundAnyoneWhenBothContributionsAreZero() {
                    stubContributions("50.00", "50.00", "0.00", "0.00");

                    service.deleteData(event);

                    verifyNoInteractions(walletService, outboxService);
                }

                @Test
                void shouldStillDeleteDataWhenNobodyIsRefunded() {
                    stubContributions("0.00", "0.00", "0.00", "0.00");

                    service.deleteData(event);

                    verify(sharedAccountFinanceDeleter).deleteAll(OWNER_ID, MEMBER_ID);
                }

                @Test
                void shouldSkipOwnerRefundAndContinueWhenOwnerWalletNoLongerExists() {
                    stubContributions("150.00", "50.00", "70.00", "20.00");
                    doThrow(new RequestedEntityNotFoundException("Wallet not found")).when(walletService)
                            .addBalanceToWallet(OWNER_ID, new BigDecimal("100.00"));

                    boolean result = service.deleteData(event);

                    assertTrue(result);
                    verify(walletService).addBalanceToWallet(MEMBER_ID, new BigDecimal("50.00"));
                    verify(outboxService, never()).save(anyString(), eq(OWNER_ID.toString()), anyString(),
                            any(SharedAccountChangeHistoryActivityEvent.class));
                    verify(outboxService).save(eq(OUTBOX_AGGREGATE), eq(MEMBER_ID.toString()), eq(OUTBOX_EVENT_TYPE),
                            any(SharedAccountChangeHistoryActivityEvent.class));
                    verify(sharedAccountFinanceDeleter).deleteAll(OWNER_ID, MEMBER_ID);
                }

                @Test
                void shouldThrowExceptionWhenRefundFailsWithUnexpectedError() {
                    stubContributions("150.00", "50.00", "70.00", "20.00");
                    doThrow(new IllegalStateException("Unexpected")).when(walletService)
                            .addBalanceToWallet(OWNER_ID, new BigDecimal("100.00"));

                    assertThrows(IllegalStateException.class, () -> service.deleteData(event));

                    verify(sharedAccountFinanceDeleter, never()).deleteAll(any(Long.class), any(Long.class));
                }

                @Test
                void shouldThrowExceptionWhenDeletionFails() {
                    stubContributions("0.00", "0.00", "0.00", "0.00");
                    doThrow(new IllegalStateException("Deletion failed")).when(sharedAccountFinanceDeleter).deleteAll(OWNER_ID, MEMBER_ID);

                    assertThrows(IllegalStateException.class, () -> service.deleteData(event));
                }
            }
        }
    }
