package com.finovara.financeservice.sharedaccount.piggybank.service;

import com.finovara.contracts.exception.badrequest.InvalidInputException;
import com.finovara.financeservice.sharedaccount.piggybank.model.SharedPiggyBank;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SharedPiggyBankTransactionServiceTest {

    @Mock
    private SharedPiggyBankManager sharedPiggyBankManager;

    @Mock
    private SharedWalletService sharedWalletService;

    @InjectMocks
    private SharedPiggyBankTransactionService sharedPiggyBankTransactionService;

    private Long userId;
    private Long piggyBankId;
    private SharedPiggyBank piggyBank;

    @BeforeEach
    void setUp() {
        userId = 1L;
        piggyBankId = 4L;
        piggyBank = SharedPiggyBank.builder()
                .id(piggyBankId)
                .amount(BigDecimal.valueOf(25))
                .goalAmount(BigDecimal.valueOf(100))
                .build();
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
        void shouldRemoveBalanceFromWalletWhenDepositIsValid() {
            when(sharedPiggyBankManager.getPiggyBankByUserId(piggyBankId, userId)).thenReturn(piggyBank);

            sharedPiggyBankTransactionService.addBalanceToPiggyBank(userId, piggyBankId, BigDecimal.valueOf(25));

            verify(sharedWalletService).removeBalanceFromWallet(userId, BigDecimal.valueOf(25));
        }

        @Test
        void shouldThrowExceptionWhenDepositAmountIsNegative() {
            when(sharedPiggyBankManager.getPiggyBankByUserId(piggyBankId, userId)).thenReturn(piggyBank);

            assertThrows(InvalidInputException.class,
                    () -> sharedPiggyBankTransactionService.addBalanceToPiggyBank(userId, piggyBankId, BigDecimal.valueOf(-5)));
        }

        @Test
        void shouldThrowExceptionWhenDepositAmountIsZero() {
            when(sharedPiggyBankManager.getPiggyBankByUserId(piggyBankId, userId)).thenReturn(piggyBank);

            assertThrows(InvalidInputException.class,
                    () -> sharedPiggyBankTransactionService.addBalanceToPiggyBank(userId, piggyBankId, BigDecimal.ZERO));
        }

        @Test
        void shouldNotRemoveBalanceFromWalletWhenDepositAmountIsInvalid() {
            when(sharedPiggyBankManager.getPiggyBankByUserId(piggyBankId, userId)).thenReturn(piggyBank);

            assertThrows(InvalidInputException.class,
                    () -> sharedPiggyBankTransactionService.addBalanceToPiggyBank(userId, piggyBankId, BigDecimal.valueOf(-5)));

            verify(sharedWalletService, never()).removeBalanceFromWallet(eq(userId), eq(BigDecimal.valueOf(-5)));
        }

        @Test
        void shouldNotChangePiggyBankAmountWhenDepositAmountIsInvalid() {
            when(sharedPiggyBankManager.getPiggyBankByUserId(piggyBankId, userId)).thenReturn(piggyBank);

            assertThrows(InvalidInputException.class,
                    () -> sharedPiggyBankTransactionService.addBalanceToPiggyBank(userId, piggyBankId, BigDecimal.valueOf(-5)));

            assertEquals(BigDecimal.valueOf(25), piggyBank.getAmount());
        }

        @Test
        void shouldPropagateExceptionWhenPiggyBankNotFound() {
            when(sharedPiggyBankManager.getPiggyBankByUserId(piggyBankId, userId))
                    .thenThrow(new RuntimeException("Piggy bank not found"));

            assertThrows(RuntimeException.class,
                    () -> sharedPiggyBankTransactionService.addBalanceToPiggyBank(userId, piggyBankId, BigDecimal.TEN));
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
        void shouldAddBalanceToWalletWhenWithdrawIsValid() {
            when(sharedPiggyBankManager.getPiggyBankByUserId(piggyBankId, userId)).thenReturn(piggyBank);

            sharedPiggyBankTransactionService.removeBalanceFromPiggyBank(userId, piggyBankId, BigDecimal.valueOf(15));

            verify(sharedWalletService).addBalanceToWallet(userId, BigDecimal.valueOf(15));
        }

        @Test
        void shouldReturnZeroPercentageWhenFullAmountIsWithdrawn() {
            when(sharedPiggyBankManager.getPiggyBankByUserId(piggyBankId, userId)).thenReturn(piggyBank);

            BigDecimal result = sharedPiggyBankTransactionService.removeBalanceFromPiggyBank(userId, piggyBankId, BigDecimal.valueOf(25));

            assertEquals(0, BigDecimal.valueOf(0.00).compareTo(result));
        }

        @Test
        void shouldThrowExceptionWhenWithdrawAmountIsNegative() {
            when(sharedPiggyBankManager.getPiggyBankByUserId(piggyBankId, userId)).thenReturn(piggyBank);

            assertThrows(InvalidInputException.class,
                    () -> sharedPiggyBankTransactionService.removeBalanceFromPiggyBank(userId, piggyBankId, BigDecimal.valueOf(-5)));
        }

        @Test
        void shouldThrowExceptionWhenWithdrawAmountIsZero() {
            when(sharedPiggyBankManager.getPiggyBankByUserId(piggyBankId, userId)).thenReturn(piggyBank);

            assertThrows(InvalidInputException.class,
                    () -> sharedPiggyBankTransactionService.removeBalanceFromPiggyBank(userId, piggyBankId, BigDecimal.ZERO));
        }

        @Test
        void shouldThrowExceptionWhenFundsAreInsufficient() {
            when(sharedPiggyBankManager.getPiggyBankByUserId(piggyBankId, userId)).thenReturn(piggyBank);

            assertThrows(InvalidInputException.class,
                    () -> sharedPiggyBankTransactionService.removeBalanceFromPiggyBank(userId, piggyBankId, BigDecimal.valueOf(100)));
        }

        @Test
        void shouldNotAddBalanceToWalletWhenFundsAreInsufficient() {
            when(sharedPiggyBankManager.getPiggyBankByUserId(piggyBankId, userId)).thenReturn(piggyBank);

            assertThrows(InvalidInputException.class,
                    () -> sharedPiggyBankTransactionService.removeBalanceFromPiggyBank(userId, piggyBankId, BigDecimal.valueOf(100)));

            verify(sharedWalletService, never()).addBalanceToWallet(eq(userId), eq(BigDecimal.valueOf(100)));
        }

        @Test
        void shouldNotChangePiggyBankAmountWhenFundsAreInsufficient() {
            when(sharedPiggyBankManager.getPiggyBankByUserId(piggyBankId, userId)).thenReturn(piggyBank);

            assertThrows(InvalidInputException.class,
                    () -> sharedPiggyBankTransactionService.removeBalanceFromPiggyBank(userId, piggyBankId, BigDecimal.valueOf(100)));

            assertEquals(BigDecimal.valueOf(25), piggyBank.getAmount());
        }

        @Test
        void shouldPropagateExceptionWhenPiggyBankNotFound() {
            when(sharedPiggyBankManager.getPiggyBankByUserId(piggyBankId, userId))
                    .thenThrow(new RuntimeException("Piggy bank not found"));

            assertThrows(RuntimeException.class,
                    () -> sharedPiggyBankTransactionService.removeBalanceFromPiggyBank(userId, piggyBankId, BigDecimal.TEN));
        }
    }
}