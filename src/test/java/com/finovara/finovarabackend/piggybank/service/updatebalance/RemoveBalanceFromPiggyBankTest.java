package com.finovara.finovarabackend.piggybank.service.transaction;

import com.finovara.finovarabackend.accountactivity.piggybank.model.PiggyBankActivityType;
import com.finovara.finovarabackend.accountactivity.piggybank.service.PiggyBankActivityService;
import com.finovara.finovarabackend.exception.badrequest.InvalidInputException;
import com.finovara.finovarabackend.piggybank.model.PiggyBank;
import com.finovara.finovarabackend.piggybank.repository.PiggyBankRepository;
import com.finovara.finovarabackend.piggybank.service.PiggyBankTransactionService;
import com.finovara.finovarabackend.user.exception.notfound.UserNotFoundException;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.usersetting.piggybank.completion.service.GoalCompletionService;
import com.finovara.finovarabackend.util.service.piggybank.manager.PiggyBankManagerService;
import com.finovara.finovarabackend.util.service.user.service.UserManagerService;
import com.finovara.finovarabackend.util.service.wallet.WalletManagerService;
import com.finovara.finovarabackend.wallet.model.Wallet;
import com.finovara.finovarabackend.wallet.repository.WalletRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PiggyBankTransactionServiceTest {

    @InjectMocks
    private PiggyBankTransactionService piggyBankTransactionService;
    @Mock
    private WalletRepository walletRepository;
    @Mock
    private PiggyBankRepository piggyBankRepository;
    @Mock
    private PiggyBankActivityService piggyBankActivityService;
    @Mock
    private GoalCompletionService goalCompletionService;
    @Mock
    private UserManagerService userManagerService;
    @Mock
    private PiggyBankManagerService piggyBankManagerService;

    @Mock
    private WalletManagerService walletManagerService;

    @Test
    void shouldRemoveBalanceSuccessfully() {
        String email = "test@test.com";
        Long piggyBankId = 1L;

        User user = new User();

        Wallet wallet = new Wallet();
        wallet.setBalance(new BigDecimal("300"));

        PiggyBank piggyBank = new PiggyBank();
        piggyBank.setAmount(new BigDecimal("200"));

        when(userManagerService.getUserByEmailOrThrow(email)).thenReturn(user);
        when(piggyBankManagerService.getPiggyBankByUserEmail(piggyBankId, email)).thenReturn(piggyBank);
        when(walletManagerService.getWalletByUserEmailOrThrow(email)).thenReturn(wallet);

        piggyBankTransactionService.removeBalanceFromPiggyBank(email, piggyBankId, new BigDecimal("100"));

        assertEquals(new BigDecimal("400"), wallet.getBalance());
        assertEquals(new BigDecimal("100"), piggyBank.getAmount());

        verify(walletRepository).save(wallet);
        verify(piggyBankRepository).save(piggyBank);

        verify(piggyBankActivityService).createPaymentPiggyBankActivity(
                eq(email),
                eq(piggyBank),
                eq(PiggyBankActivityType.AMOUNT_REMOVED_FROM_PIGGY_BANK),
                eq(new BigDecimal("100"))
        );
    }

    @Test
    void shouldThrowWhenInsufficientPiggyBankFunds() {
        String email = "test@test.com";
        Long piggyBankId = 1L;

        User user = new User();

        Wallet wallet = new Wallet();
        wallet.setBalance(new BigDecimal("300"));

        PiggyBank piggyBank = new PiggyBank();
        piggyBank.setAmount(new BigDecimal("50"));

        when(userManagerService.getUserByEmailOrThrow(email)).thenReturn(user);
        when(piggyBankManagerService.getPiggyBankByUserEmail(piggyBankId, email)).thenReturn(piggyBank);
        when(walletManagerService.getWalletByUserEmailOrThrow(email)).thenReturn(wallet);

        assertThrows(InvalidInputException.class, () -> piggyBankTransactionService.removeBalanceFromPiggyBank(email, piggyBankId, new BigDecimal("100")));

        verify(piggyBankRepository, never()).save(any());
        verify(walletRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenUserNotFound_remove() {
        String email = "test@test.com";
        Long piggyBankId = 1L;

        when(userManagerService.getUserByEmailOrThrow(email)).thenThrow(new UserNotFoundException("User not found"));

        assertThrows(UserNotFoundException.class, () -> piggyBankTransactionService.removeBalanceFromPiggyBank(email, piggyBankId, new BigDecimal("100")));

        verify(piggyBankRepository, never()).save(any());
        verify(walletRepository, never()).save(any());
    }
}