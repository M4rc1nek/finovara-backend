package com.finovara.financeservice.sharedaccount.piggybank.service;

import com.finovara.contracts.sharedaccount.event.activity.finance.SharedAccountPiggyBankDepositActivityEvent;
import com.finovara.contracts.outbox.OutboxService;
import com.finovara.financeservice.sharedaccount.participants.SharedAccountParticipantsResponse;
import com.finovara.financeservice.sharedaccount.participants.SharedAccountParticipantsService;
import com.finovara.financeservice.sharedaccount.piggybank.model.SharedPiggyBank;
import com.finovara.financeservice.sharedaccount.settings.piggybank.goalachieved.service.GoalAchievedNotificationService;
import com.finovara.financeservice.sharedaccount.wallet.service.SharedWalletService;
import com.finovara.financeservice.util.transaction.piggybank.manager.SharedPiggyBankManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SharedPiggyBankTransactionServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long PIGGY_BANK_ID = 5L;
    private static final Long OWNER_ID = 10L;
    private static final Long MEMBER_ID = 20L;
    private static final BigDecimal INITIAL_AMOUNT = new BigDecimal("100.00");
    private static final BigDecimal GOAL_AMOUNT = new BigDecimal("1000.00");
    private static final BigDecimal AMOUNT = new BigDecimal("50.00");

    @Mock
    private SharedPiggyBankManager sharedPiggyBankManager;

    @Mock
    private SharedWalletService sharedWalletService;

    @Mock
    private GoalAchievedNotificationService goalAchievedNotificationService;

    @Mock
    private SharedAccountParticipantsService sharedAccountParticipantsService;

    @Mock
    private OutboxService outboxService;

    @Mock
    private SharedAccountParticipantsResponse participants;

    @InjectMocks
    private SharedPiggyBankTransactionService sharedPiggyBankTransactionService;

    private SharedPiggyBank piggyBank;

    @BeforeEach
    void setUp() {
        piggyBank = spy(SharedPiggyBank.builder().amount(INITIAL_AMOUNT).goalAmount(GOAL_AMOUNT).build());
        when(sharedPiggyBankManager.getPiggyBankByUserId(PIGGY_BANK_ID, USER_ID)).thenReturn(piggyBank);
    }

    @Nested
    class AddBalanceToPiggyBank {

        @BeforeEach
        void setUp() {
            when(sharedAccountParticipantsService.getParticipants(USER_ID)).thenReturn(participants);
            when(participants.ownerId()).thenReturn(OWNER_ID);
            when(participants.memberId()).thenReturn(MEMBER_ID);
        }

        @Test
        void shouldIncreasePiggyBankAmountWhenDepositIsValid() {
            sharedPiggyBankTransactionService.addBalanceToPiggyBank(USER_ID, PIGGY_BANK_ID, AMOUNT);

            assertEquals(new BigDecimal("150.00"), piggyBank.getAmount());
        }

        @Test
        void shouldRemoveDepositFromWalletWhenDepositIsValid() {
            sharedPiggyBankTransactionService.addBalanceToPiggyBank(USER_ID, PIGGY_BANK_ID, AMOUNT);

            verify(sharedWalletService).removeBalanceFromWallet(USER_ID, AMOUNT);
        }

        @Test
        void shouldSaveDepositEventToOutboxWhenDepositIsValid() {
            sharedPiggyBankTransactionService.addBalanceToPiggyBank(USER_ID, PIGGY_BANK_ID, AMOUNT);

            verify(outboxService).save(eq("SharedAccountPiggyBank"), eq(PIGGY_BANK_ID.toString()), eq("shared-account.piggybank.deposit.added"), any(SharedAccountPiggyBankDepositActivityEvent.class));
        }

        @Test
        void shouldHandleGoalAchievedWhenDepositIsValid() {
            sharedPiggyBankTransactionService.addBalanceToPiggyBank(USER_ID, PIGGY_BANK_ID, AMOUNT);

            verify(goalAchievedNotificationService).handleGoalAchieved(USER_ID, piggyBank);
        }

        @Test
        void shouldHandleGoalAchievedAfterOutboxSaveWhenDepositIsValid() {
            InOrder inOrder = inOrder(outboxService, goalAchievedNotificationService);

            sharedPiggyBankTransactionService.addBalanceToPiggyBank(USER_ID, PIGGY_BANK_ID, AMOUNT);

            inOrder.verify(outboxService).save(anyString(), anyString(), anyString(), any());
            inOrder.verify(goalAchievedNotificationService).handleGoalAchieved(USER_ID, piggyBank);
        }
    }

    @Nested
    class AddBalanceToPiggyBankValidation {

        @BeforeEach
        void setUp() {
            when(sharedAccountParticipantsService.getParticipants(USER_ID)).thenReturn(participants);
        }

        @ParameterizedTest
        @ValueSource(strings = {"0", "-1.00"})
        void shouldThrowExceptionWhenDepositAmountIsNotPositive(BigDecimal amount) {
            assertThrows(RuntimeException.class, () -> sharedPiggyBankTransactionService.addBalanceToPiggyBank(USER_ID, PIGGY_BANK_ID, amount));
        }

        @Test
        void shouldThrowExceptionWhenDepositAmountIsNull() {
            assertThrows(RuntimeException.class, () -> sharedPiggyBankTransactionService.addBalanceToPiggyBank(USER_ID, PIGGY_BANK_ID, null));
        }

        @Test
        void shouldNotChangeStateWhenDepositAmountIsInvalid() {
            assertThrows(RuntimeException.class, () -> sharedPiggyBankTransactionService.addBalanceToPiggyBank(USER_ID, PIGGY_BANK_ID, new BigDecimal("-5.00")));

            assertEquals(INITIAL_AMOUNT, piggyBank.getAmount());
            verifyNoInteractions(sharedWalletService, outboxService, goalAchievedNotificationService);
        }
    }

    @Nested
    class AddBalanceToPiggyBankExceptions {

        @Test
        void shouldThrowExceptionWhenPiggyBankLookupFails() {
            when(sharedPiggyBankManager.getPiggyBankByUserId(PIGGY_BANK_ID, USER_ID)).thenThrow(new IllegalStateException());

            assertThrows(IllegalStateException.class, () -> sharedPiggyBankTransactionService.addBalanceToPiggyBank(USER_ID, PIGGY_BANK_ID, AMOUNT));
            verifyNoInteractions(sharedAccountParticipantsService, sharedWalletService, outboxService, goalAchievedNotificationService);
        }

        @Test
        void shouldThrowExceptionWhenParticipantsLookupFails() {
            when(sharedAccountParticipantsService.getParticipants(USER_ID)).thenThrow(new IllegalStateException());

            assertThrows(IllegalStateException.class, () -> sharedPiggyBankTransactionService.addBalanceToPiggyBank(USER_ID, PIGGY_BANK_ID, AMOUNT));
            verifyNoInteractions(sharedWalletService, outboxService, goalAchievedNotificationService);
        }

        @Test
        void shouldThrowExceptionWhenWalletWithdrawalFails() {
            when(sharedAccountParticipantsService.getParticipants(USER_ID)).thenReturn(participants);
            doThrow(new IllegalStateException()).when(sharedWalletService).removeBalanceFromWallet(USER_ID, AMOUNT);

            assertThrows(IllegalStateException.class, () -> sharedPiggyBankTransactionService.addBalanceToPiggyBank(USER_ID, PIGGY_BANK_ID, AMOUNT));
            verifyNoInteractions(outboxService, goalAchievedNotificationService);
        }

        @Test
        void shouldThrowExceptionWhenOutboxSaveFails() {
            when(sharedAccountParticipantsService.getParticipants(USER_ID)).thenReturn(participants);
            when(participants.ownerId()).thenReturn(OWNER_ID);
            when(participants.memberId()).thenReturn(MEMBER_ID);
            doThrow(new IllegalStateException()).when(outboxService).save(anyString(), anyString(), anyString(), any());

            assertThrows(IllegalStateException.class, () -> sharedPiggyBankTransactionService.addBalanceToPiggyBank(USER_ID, PIGGY_BANK_ID, AMOUNT));
            verifyNoInteractions(goalAchievedNotificationService);
        }

        @Test
        void shouldThrowExceptionWhenGoalAchievedHandlingFails() {
            when(sharedAccountParticipantsService.getParticipants(USER_ID)).thenReturn(participants);
            when(participants.ownerId()).thenReturn(OWNER_ID);
            when(participants.memberId()).thenReturn(MEMBER_ID);
            doThrow(new IllegalStateException()).when(goalAchievedNotificationService).handleGoalAchieved(USER_ID, piggyBank);

            assertThrows(IllegalStateException.class, () -> sharedPiggyBankTransactionService.addBalanceToPiggyBank(USER_ID, PIGGY_BANK_ID, AMOUNT));
        }
    }

    @Nested
    class RemoveBalanceFromPiggyBank {

        @Test
        void shouldDecreasePiggyBankAmountWhenWithdrawalIsValid() {
            sharedPiggyBankTransactionService.removeBalanceFromPiggyBank(USER_ID, PIGGY_BANK_ID, AMOUNT);

            assertEquals(new BigDecimal("50.00"), piggyBank.getAmount());
        }

        @Test
        void shouldAddWithdrawnAmountToWalletWhenWithdrawalIsValid() {
            sharedPiggyBankTransactionService.removeBalanceFromPiggyBank(USER_ID, PIGGY_BANK_ID, AMOUNT);

            verify(sharedWalletService).addBalanceToWallet(USER_ID, AMOUNT);
        }

        @Test
        void shouldAllowWithdrawingWholeAmountWhenAmountEqualsBalance() {
            BigDecimal result = sharedPiggyBankTransactionService.removeBalanceFromPiggyBank(USER_ID, PIGGY_BANK_ID, INITIAL_AMOUNT);

            assertEquals(new BigDecimal("0.00"), piggyBank.getAmount());
            assertEquals(new BigDecimal("0.00"), result);
        }

        @Test
        void shouldResetGoalAchievedNotifiedWhenAmountDropsBelowGoal() {
            sharedPiggyBankTransactionService.removeBalanceFromPiggyBank(USER_ID, PIGGY_BANK_ID, AMOUNT);

            verify(piggyBank).setGoalAchievedNotified(false);
        }

        @Test
        void shouldNotResetGoalAchievedNotifiedWhenAmountStaysAboveGoal() {
            piggyBank.setAmount(new BigDecimal("2000.00"));

            sharedPiggyBankTransactionService.removeBalanceFromPiggyBank(USER_ID, PIGGY_BANK_ID, AMOUNT);

            verify(piggyBank, never()).setGoalAchievedNotified(anyBoolean());
        }

        @Test
        void shouldNotResetGoalAchievedNotifiedWhenPiggyBankHasNoGoal() {
            SharedPiggyBank piggyBankWithoutGoal = spy(SharedPiggyBank.builder().amount(INITIAL_AMOUNT).build());
            when(sharedPiggyBankManager.getPiggyBankByUserId(PIGGY_BANK_ID, USER_ID)).thenReturn(piggyBankWithoutGoal);

            sharedPiggyBankTransactionService.removeBalanceFromPiggyBank(USER_ID, PIGGY_BANK_ID, AMOUNT);

            verify(piggyBankWithoutGoal, never()).setGoalAchievedNotified(anyBoolean());
            verify(sharedWalletService).addBalanceToWallet(USER_ID, AMOUNT);
        }

        @Test
        void shouldNotUseOutboxOrParticipantsWhenWithdrawalIsValid() {
            sharedPiggyBankTransactionService.removeBalanceFromPiggyBank(USER_ID, PIGGY_BANK_ID, AMOUNT);

            verifyNoInteractions(outboxService, goalAchievedNotificationService, sharedAccountParticipantsService);
        }
    }

    @Nested
    class RemoveBalanceFromPiggyBankValidation {

        @ParameterizedTest
        @ValueSource(strings = {"0", "-1.00"})
        void shouldThrowExceptionWhenWithdrawalAmountIsNotPositive(BigDecimal amount) {
            assertThrows(RuntimeException.class, () -> sharedPiggyBankTransactionService.removeBalanceFromPiggyBank(USER_ID, PIGGY_BANK_ID, amount));
        }

        @Test
        void shouldThrowExceptionWhenWithdrawalAmountIsNull() {
            assertThrows(RuntimeException.class, () -> sharedPiggyBankTransactionService.removeBalanceFromPiggyBank(USER_ID, PIGGY_BANK_ID, null));
        }

        @Test
        void shouldThrowExceptionWhenWithdrawalExceedsPiggyBankAmount() {
            assertThrows(RuntimeException.class, () -> sharedPiggyBankTransactionService.removeBalanceFromPiggyBank(USER_ID, PIGGY_BANK_ID, new BigDecimal("100.01")));
        }

        @Test
        void shouldNotChangeStateWhenWithdrawalExceedsPiggyBankAmount() {
            assertThrows(RuntimeException.class, () -> sharedPiggyBankTransactionService.removeBalanceFromPiggyBank(USER_ID, PIGGY_BANK_ID, new BigDecimal("100.01")));

            assertEquals(INITIAL_AMOUNT, piggyBank.getAmount());
            verifyNoInteractions(sharedWalletService);
        }
    }

    @Nested
    class RemoveBalanceFromPiggyBankExceptions {

        @Test
        void shouldThrowExceptionWhenPiggyBankLookupFails() {
            when(sharedPiggyBankManager.getPiggyBankByUserId(PIGGY_BANK_ID, USER_ID)).thenThrow(new IllegalStateException());

            assertThrows(IllegalStateException.class, () -> sharedPiggyBankTransactionService.removeBalanceFromPiggyBank(USER_ID, PIGGY_BANK_ID, AMOUNT));
            verifyNoInteractions(sharedWalletService);
        }

        @Test
        void shouldThrowExceptionWhenWalletDepositFails() {
            doThrow(new IllegalStateException()).when(sharedWalletService).addBalanceToWallet(USER_ID, AMOUNT);

            assertThrows(IllegalStateException.class, () -> sharedPiggyBankTransactionService.removeBalanceFromPiggyBank(USER_ID, PIGGY_BANK_ID, AMOUNT));
        }
    }
}