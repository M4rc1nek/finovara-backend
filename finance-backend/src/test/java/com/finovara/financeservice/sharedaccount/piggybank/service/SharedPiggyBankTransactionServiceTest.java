package com.finovara.financeservice.sharedaccount.piggybank.service;

import com.finovara.contracts.exception.badrequest.InvalidInputException;
import com.finovara.financeservice.sharedaccount.piggybank.model.SharedPiggyBank;
import com.finovara.financeservice.sharedaccount.settings.piggybank.goalachieved.service.GoalAchievedNotificationService;
import com.finovara.financeservice.sharedaccount.wallet.service.SharedWalletService;
import com.finovara.financeservice.util.piggybank.manager.SharedPiggyBankManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SharedPiggyBankTransactionServiceTest {

    @Mock
    private SharedPiggyBankManager sharedPiggyBankManager;

    @Mock
    private SharedWalletService sharedWalletService;

    @Mock
    private GoalAchievedNotificationService goalAchievedNotificationService;

    @InjectMocks
    private SharedPiggyBankTransactionService sharedPiggyBankTransactionService;

    private Long userId;
    private Long piggyBankId;
    private SharedPiggyBank piggyBank;

    @BeforeEach
    void setUp() {
        userId = 1L;
        piggyBankId = 4L;
        piggyBank = SharedPiggyBank.builder().id(piggyBankId).amount(BigDecimal.valueOf(25)).goalAmount(BigDecimal.valueOf(100)).build();
    }

    @Nested
    class AddBalanceToPiggyBank {

        @Test
        void shouldIncreasePiggyBankAmountWhenDepositIsValid() {
            when(sharedPiggyBankManager.getPiggyBankByUserId(piggyBankId, userId)).thenReturn(piggyBank);

            sharedPiggyBankTransactionService.addBalanceToPiggyBank(userId, piggyBankId, BigDecimal.valueOf(25));

            assertEquals(BigDecimal.valueOf(50), piggyBank.getAmount());
        }

        @Test
        void shouldReturnCalculatedPercentageWhenDepositIsValid() {
            when(sharedPiggyBankManager.getPiggyBankByUserId(piggyBankId, userId)).thenReturn(piggyBank);

            BigDecimal result = sharedPiggyBankTransactionService.addBalanceToPiggyBank(userId, piggyBankId, BigDecimal.valueOf(25));

            assertEquals(new BigDecimal("5000.00"), result);
        }

        @Test
        void shouldRemoveBalanceFromWalletWhenDepositIsValid() {
            when(sharedPiggyBankManager.getPiggyBankByUserId(piggyBankId, userId)).thenReturn(piggyBank);

            sharedPiggyBankTransactionService.addBalanceToPiggyBank(userId, piggyBankId, BigDecimal.valueOf(25));

            verify(sharedWalletService).removeBalanceFromWallet(userId, BigDecimal.valueOf(25));
        }

        @Test
        void shouldNotifyGoalAchievementServiceWhenDepositIsValid() {
            when(sharedPiggyBankManager.getPiggyBankByUserId(piggyBankId, userId)).thenReturn(piggyBank);

            sharedPiggyBankTransactionService.addBalanceToPiggyBank(userId, piggyBankId, BigDecimal.valueOf(25));

            verify(goalAchievedNotificationService).handleGoalAchieved(userId, piggyBank);
        }

        @Test
        void shouldThrowExceptionWhenDepositAmountIsNegative() {
            when(sharedPiggyBankManager.getPiggyBankByUserId(piggyBankId, userId)).thenReturn(piggyBank);
            BigDecimal invalidAmount = BigDecimal.valueOf(-5);

            assertThrows(InvalidInputException.class, () -> sharedPiggyBankTransactionService.addBalanceToPiggyBank(userId, piggyBankId, invalidAmount));

            verify(goalAchievedNotificationService, never()).handleGoalAchieved(userId, piggyBank);
        }

        @Test
        void shouldThrowExceptionWhenDepositAmountIsZero() {
            when(sharedPiggyBankManager.getPiggyBankByUserId(piggyBankId, userId)).thenReturn(piggyBank);
            BigDecimal invalidAmount = BigDecimal.ZERO;

            assertThrows(InvalidInputException.class, () -> sharedPiggyBankTransactionService.addBalanceToPiggyBank(userId, piggyBankId, invalidAmount));

            verify(goalAchievedNotificationService, never()).handleGoalAchieved(userId, piggyBank);
        }

        @Test
        void shouldNotRemoveBalanceFromWalletWhenDepositAmountIsInvalid() {
            when(sharedPiggyBankManager.getPiggyBankByUserId(piggyBankId, userId)).thenReturn(piggyBank);
            BigDecimal invalidAmount = BigDecimal.valueOf(-5);

            assertThrows(InvalidInputException.class, () -> sharedPiggyBankTransactionService.addBalanceToPiggyBank(userId, piggyBankId, invalidAmount));

            verify(sharedWalletService, never()).removeBalanceFromWallet(userId, invalidAmount);
        }

        @Test
        void shouldThrowExceptionWhenPiggyBankAmountDepositAmountIsInvalid() {
            when(sharedPiggyBankManager.getPiggyBankByUserId(piggyBankId, userId)).thenReturn(piggyBank);
            BigDecimal invalidAmount = BigDecimal.valueOf(-5);

            assertThrows(InvalidInputException.class, () -> sharedPiggyBankTransactionService.addBalanceToPiggyBank(userId, piggyBankId, invalidAmount));

            assertEquals(BigDecimal.valueOf(25), piggyBank.getAmount());
        }

        @Test
        void shouldPropagateExceptionWhenPiggyBankNotFound() {
            when(sharedPiggyBankManager.getPiggyBankByUserId(piggyBankId, userId)).thenThrow(new RuntimeException("Piggy bank not found"));

            assertThrows(RuntimeException.class, () -> sharedPiggyBankTransactionService.addBalanceToPiggyBank(userId, piggyBankId, BigDecimal.TEN));
        }
    }

    @Nested
    class RemoveBalanceFromPiggyBank {

        @Test
        void shouldDecreasePiggyBankAmountWhenWithdrawIsValid() {
            when(sharedPiggyBankManager.getPiggyBankByUserId(piggyBankId, userId)).thenReturn(piggyBank);

            sharedPiggyBankTransactionService.removeBalanceFromPiggyBank(userId, piggyBankId, BigDecimal.valueOf(15));

            assertEquals(BigDecimal.valueOf(10), piggyBank.getAmount());
        }

        @Test
        void shouldReturnCalculatedPercentageWhenWithdrawIsValid() {
            when(sharedPiggyBankManager.getPiggyBankByUserId(piggyBankId, userId)).thenReturn(piggyBank);

            BigDecimal result = sharedPiggyBankTransactionService.removeBalanceFromPiggyBank(userId, piggyBankId, BigDecimal.valueOf(15));

            assertEquals(new BigDecimal("1000.00"), result);
        }

        @Test
        void shouldAddBalanceToWalletWhenWithdrawIsValid() {
            when(sharedPiggyBankManager.getPiggyBankByUserId(piggyBankId, userId)).thenReturn(piggyBank);

            sharedPiggyBankTransactionService.removeBalanceFromPiggyBank(userId, piggyBankId, BigDecimal.valueOf(15));

            verify(sharedWalletService).addBalanceToWallet(userId, BigDecimal.valueOf(15));
        }

        @Test
        void shouldReturnZeroPercentageWhenFullAmountIsWithdrawn() {
            when(sharedPiggyBankManager.getPiggyBankByUserId(piggyBankId, userId)).thenReturn(piggyBank);

            BigDecimal result = sharedPiggyBankTransactionService.removeBalanceFromPiggyBank(userId, piggyBankId, BigDecimal.valueOf(25));

            assertEquals(0, BigDecimal.ZERO.compareTo(result));
        }

        @Test
        void shouldThrowExceptionWhenWithdrawAmountIsNegative() {
            when(sharedPiggyBankManager.getPiggyBankByUserId(piggyBankId, userId)).thenReturn(piggyBank);
            BigDecimal invalidAmount = BigDecimal.valueOf(-5);

            assertThrows(InvalidInputException.class, () -> sharedPiggyBankTransactionService.removeBalanceFromPiggyBank(userId, piggyBankId, invalidAmount));
        }

        @Test
        void shouldThrowExceptionWhenWithdrawAmountIsZero() {
            when(sharedPiggyBankManager.getPiggyBankByUserId(piggyBankId, userId)).thenReturn(piggyBank);
            BigDecimal invalidAmount = BigDecimal.ZERO;

            assertThrows(InvalidInputException.class, () -> sharedPiggyBankTransactionService.removeBalanceFromPiggyBank(userId, piggyBankId, invalidAmount));
        }

        @Test
        void shouldThrowExceptionWhenFundsAreInsufficient() {
            when(sharedPiggyBankManager.getPiggyBankByUserId(piggyBankId, userId)).thenReturn(piggyBank);
            BigDecimal invalidAmount = BigDecimal.valueOf(100);

            assertThrows(InvalidInputException.class, () -> sharedPiggyBankTransactionService.removeBalanceFromPiggyBank(userId, piggyBankId, invalidAmount));
        }

        @Test
        void shouldResetGoalAchievedNotificationWhenBalanceFallsBelowGoal() {
            piggyBank.setAmount(BigDecimal.valueOf(120));
            piggyBank.setGoalAmount(BigDecimal.valueOf(100));
            piggyBank.setGoalAchievedNotified(true);
            when(sharedPiggyBankManager.getPiggyBankByUserId(piggyBankId, userId)).thenReturn(piggyBank);

            sharedPiggyBankTransactionService.removeBalanceFromPiggyBank(userId, piggyBankId, BigDecimal.valueOf(25));

            assertEquals(BigDecimal.valueOf(95), piggyBank.getAmount());
            assertFalse(piggyBank.isGoalAchievedNotified());
        }

        @Test
        void shouldKeepGoalAchievedNotificationWhenGoalAmountIsNull() {
            piggyBank.setGoalAmount(null);
            piggyBank.setGoalAchievedNotified(true);
            when(sharedPiggyBankManager.getPiggyBankByUserId(piggyBankId, userId)).thenReturn(piggyBank);

            sharedPiggyBankTransactionService.removeBalanceFromPiggyBank(userId, piggyBankId, BigDecimal.valueOf(15));

            assertTrue(piggyBank.isGoalAchievedNotified());
        }

        @Test
        void shouldKeepGoalAchievedNotificationWhenBalanceStaysAboveGoal() {
            piggyBank.setAmount(BigDecimal.valueOf(120));
            piggyBank.setGoalAmount(BigDecimal.valueOf(100));
            piggyBank.setGoalAchievedNotified(true);
            when(sharedPiggyBankManager.getPiggyBankByUserId(piggyBankId, userId)).thenReturn(piggyBank);

            sharedPiggyBankTransactionService.removeBalanceFromPiggyBank(userId, piggyBankId, BigDecimal.valueOf(10));

            assertTrue(piggyBank.isGoalAchievedNotified());
        }

        @Test
        void shouldNotAddBalanceToWalletWhenFundsAreInsufficient() {
            when(sharedPiggyBankManager.getPiggyBankByUserId(piggyBankId, userId)).thenReturn(piggyBank);
            BigDecimal invalidAmount = BigDecimal.valueOf(100);

            assertThrows(InvalidInputException.class, () -> sharedPiggyBankTransactionService.removeBalanceFromPiggyBank(userId, piggyBankId, invalidAmount));

            verify(sharedWalletService, never()).addBalanceToWallet(userId, invalidAmount);
        }

        @Test
        void shouldNotChangePiggyBankAmountWhenFundsAreInsufficient() {
            when(sharedPiggyBankManager.getPiggyBankByUserId(piggyBankId, userId)).thenReturn(piggyBank);
            BigDecimal invalidAmount = BigDecimal.valueOf(100);

            assertThrows(InvalidInputException.class, () -> sharedPiggyBankTransactionService.removeBalanceFromPiggyBank(userId, piggyBankId, invalidAmount));

            assertEquals(BigDecimal.valueOf(25), piggyBank.getAmount());
        }

        @Test
        void shouldPropagateExceptionWhenPiggyBankNotFound() {
            when(sharedPiggyBankManager.getPiggyBankByUserId(piggyBankId, userId)).thenThrow(new RuntimeException("Piggy bank not found"));

            assertThrows(RuntimeException.class, () -> sharedPiggyBankTransactionService.removeBalanceFromPiggyBank(userId, piggyBankId, BigDecimal.TEN));
        }
    }
}
