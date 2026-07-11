package com.finovara.financeservice.piggybank.service;

import com.finovara.contracts.event.activity.piggybank.PiggyBankActivityEvent;
import com.finovara.contracts.event.notification.piggybank.PiggyBankProgressEvent;
import com.finovara.contracts.exception.badrequest.InvalidInputException;
import com.finovara.contracts.model.activity.PiggyBankActivityType;
import com.finovara.contracts.outbox.OutboxService;
import com.finovara.financeservice.piggybank.model.PiggyBank;
import com.finovara.financeservice.settings.piggybank.completion.service.GoalCompletionService;
import com.finovara.financeservice.util.piggybank.manager.PiggyBankManagerService;
import com.finovara.financeservice.wallet.service.WalletService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PiggyBankTransactionServiceTest {

    @InjectMocks
    private PiggyBankTransactionService piggyBankTransactionService;

    @Mock
    private PiggyBankManagerService piggyBankManagerService;
    @Mock
    private OutboxService outboxService;
    @Mock
    private GoalCompletionService goalCompletionService;
    @Mock
    private WalletService walletService;

    private PiggyBank piggyBank;

    private final Long userId = 1L;
    private final Long piggyBankId = 10L;

    @BeforeEach
    void setUp() {
        piggyBank = new PiggyBank();
        piggyBank.setAmount(BigDecimal.ZERO);
        piggyBank.setGoalAmount(new BigDecimal("1000"));
    }

    @Nested
    class AddBalanceToPiggyBankTests {

        @Test
        void shouldAddBalanceSuccessfully() {
            when(piggyBankManagerService.getPiggyBankByUserId(piggyBankId, userId)).thenReturn(piggyBank);

            piggyBankTransactionService.addBalanceToPiggyBank(userId, piggyBankId, new BigDecimal("100"), PiggyBankActivityType.AMOUNT_ADDED_TO_PIGGY_BANK_DIRECTLY);

            assertEquals(new BigDecimal("100"), piggyBank.getAmount());
            verify(walletService).removeBalanceFromWallet(userId, new BigDecimal("100"));
        }

        @Test
        void shouldSaveActivityEventToOutbox() {
            when(piggyBankManagerService.getPiggyBankByUserId(piggyBankId, userId)).thenReturn(piggyBank);

            piggyBankTransactionService.addBalanceToPiggyBank(userId, piggyBankId, new BigDecimal("100"), PiggyBankActivityType.AMOUNT_ADDED_TO_PIGGY_BANK_DIRECTLY);

            ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
            verify(outboxService).save(
                    eq("PiggyBank"),
                    eq(piggyBankId.toString()),
                    eq("activity.piggybank"),
                    payloadCaptor.capture()
            );
            PiggyBankActivityEvent event = (PiggyBankActivityEvent) payloadCaptor.getValue();
            assertEquals(PiggyBankActivityType.AMOUNT_ADDED_TO_PIGGY_BANK_DIRECTLY, event.type());
        }

        @Test
        void shouldSaveProgressEventToOutbox() {
            when(piggyBankManagerService.getPiggyBankByUserId(piggyBankId, userId)).thenReturn(piggyBank);

            piggyBankTransactionService.addBalanceToPiggyBank(userId, piggyBankId, new BigDecimal("100"), PiggyBankActivityType.AMOUNT_ADDED_TO_PIGGY_BANK_DIRECTLY);

            ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
            verify(outboxService).save(
                    eq("PiggyBank"),
                    eq(piggyBankId.toString()),
                    eq("piggybank.calculate-progress"),
                    payloadCaptor.capture()
            );
            PiggyBankProgressEvent event = (PiggyBankProgressEvent) payloadCaptor.getValue();
            assertEquals(piggyBankId, event.piggyBankId());
        }

        @Test
        void shouldCallGoalCompletionWhenGoalReached() {
            piggyBank.setAmount(new BigDecimal("950"));
            when(piggyBankManagerService.getPiggyBankByUserId(piggyBankId, userId)).thenReturn(piggyBank);

            piggyBankTransactionService.addBalanceToPiggyBank(userId, piggyBankId, new BigDecimal("50"), PiggyBankActivityType.AMOUNT_ADDED_TO_PIGGY_BANK_DIRECTLY);

            assertEquals(new BigDecimal("1000"), piggyBank.getAmount());
            verify(goalCompletionService).handleGoalCompletion(userId);
        }

        @Test
        void shouldNotCallGoalCompletionWhenGoalNotReached() {
            when(piggyBankManagerService.getPiggyBankByUserId(piggyBankId, userId)).thenReturn(piggyBank);

            piggyBankTransactionService.addBalanceToPiggyBank(userId, piggyBankId, new BigDecimal("100"), PiggyBankActivityType.AMOUNT_ADDED_TO_PIGGY_BANK_DIRECTLY);

            verifyNoInteractions(goalCompletionService);
        }

        @Test
        void shouldPropagateExceptionAndSkipOutboxWhenWalletHasInsufficientFunds() {
            when(piggyBankManagerService.getPiggyBankByUserId(piggyBankId, userId)).thenReturn(piggyBank);
            doThrow(new InvalidInputException("Insufficient funds"))
                    .when(walletService).removeBalanceFromWallet(userId, new BigDecimal("100"));

            assertThrows(InvalidInputException.class, () ->
                    piggyBankTransactionService.addBalanceToPiggyBank(userId, piggyBankId, new BigDecimal("100"), PiggyBankActivityType.AMOUNT_ADDED_TO_PIGGY_BANK_DIRECTLY));

            verifyNoInteractions(outboxService);
            verifyNoInteractions(goalCompletionService);
        }

        @Test
        void shouldThrowWhenAmountIsInvalid() {
            when(piggyBankManagerService.getPiggyBankByUserId(piggyBankId, userId)).thenReturn(piggyBank);

            assertThrows(InvalidInputException.class, () ->
                    piggyBankTransactionService.addBalanceToPiggyBank(userId, piggyBankId, new BigDecimal("-10"), PiggyBankActivityType.AMOUNT_ADDED_TO_PIGGY_BANK_DIRECTLY));

            verifyNoInteractions(walletService);
            verifyNoInteractions(outboxService);
        }
    }

    @Nested
    class RemoveBalanceFromPiggyBankTests {

        @Test
        void shouldRemoveBalanceSuccessfully() {
            piggyBank.setAmount(new BigDecimal("200"));
            when(piggyBankManagerService.getPiggyBankByUserId(piggyBankId, userId)).thenReturn(piggyBank);

            piggyBankTransactionService.removeBalanceFromPiggyBank(userId, piggyBankId, new BigDecimal("100"));

            assertEquals(new BigDecimal("100"), piggyBank.getAmount());
            verify(walletService).addBalanceToWallet(userId, new BigDecimal("100"));
            verifyNoInteractions(goalCompletionService);
        }

        @Test
        void shouldSaveActivityEventToOutbox() {
            piggyBank.setAmount(new BigDecimal("200"));
            when(piggyBankManagerService.getPiggyBankByUserId(piggyBankId, userId)).thenReturn(piggyBank);

            piggyBankTransactionService.removeBalanceFromPiggyBank(userId, piggyBankId, new BigDecimal("100"));

            ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
            verify(outboxService).save(
                    eq("PiggyBank"),
                    eq(piggyBankId.toString()),
                    eq("activity.piggybank"),
                    payloadCaptor.capture()
            );
            PiggyBankActivityEvent event = (PiggyBankActivityEvent) payloadCaptor.getValue();
            assertEquals(PiggyBankActivityType.AMOUNT_REMOVED_FROM_PIGGY_BANK, event.type());
        }

        @Test
        void shouldSaveProgressEventToOutbox() {
            piggyBank.setAmount(new BigDecimal("200"));
            when(piggyBankManagerService.getPiggyBankByUserId(piggyBankId, userId)).thenReturn(piggyBank);

            piggyBankTransactionService.removeBalanceFromPiggyBank(userId, piggyBankId, new BigDecimal("100"));

            ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
            verify(outboxService).save(
                    eq("PiggyBank"),
                    eq(piggyBankId.toString()),
                    eq("piggybank.calculate-progress"),
                    payloadCaptor.capture()
            );
            PiggyBankProgressEvent event = (PiggyBankProgressEvent) payloadCaptor.getValue();
            assertEquals(piggyBankId, event.piggyBankId());
        }

        @Test
        void shouldThrowWhenInsufficientPiggyBankFunds() {
            piggyBank.setAmount(new BigDecimal("50"));
            when(piggyBankManagerService.getPiggyBankByUserId(piggyBankId, userId)).thenReturn(piggyBank);

            assertThrows(InvalidInputException.class, () ->
                    piggyBankTransactionService.removeBalanceFromPiggyBank(userId, piggyBankId, new BigDecimal("100")));

            assertEquals(new BigDecimal("50"), piggyBank.getAmount());
            verifyNoInteractions(walletService, outboxService, goalCompletionService);
        }

        @Test
        void shouldThrowWhenAmountIsInvalid() {
            piggyBank.setAmount(new BigDecimal("200"));
            when(piggyBankManagerService.getPiggyBankByUserId(piggyBankId, userId)).thenReturn(piggyBank);

            assertThrows(InvalidInputException.class, () ->
                    piggyBankTransactionService.removeBalanceFromPiggyBank(userId, piggyBankId, new BigDecimal("-10")));

            verifyNoInteractions(walletService, outboxService, goalCompletionService);
        }
    }
}