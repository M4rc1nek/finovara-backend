package com.finovara.finovarabackend.piggybank.service.updatebalance;

import com.finovara.finovarabackend.accountactivity.piggybank.model.PiggyBankActivityType;
import com.finovara.finovarabackend.accountactivity.piggybank.service.PiggyBankActivityService;
import com.finovara.finovarabackend.exception.badrequest.InvalidInputException;
import com.finovara.finovarabackend.piggybank.model.PiggyBank;
import com.finovara.finovarabackend.piggybank.repository.PiggyBankRepository;
import com.finovara.finovarabackend.piggybank.service.PiggyBankTransactionService;
import com.finovara.finovarabackend.user.exception.notfound.UserNotFoundException;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.usersetting.piggybank.completion.service.GoalCompletionService;
import com.finovara.finovarabackend.util.piggybank.manager.PiggyBankManagerService;
import com.finovara.finovarabackend.util.user.service.UserManagerService;
import com.finovara.finovarabackend.util.wallet.WalletManagerService;
import com.finovara.finovarabackend.wallet.model.Wallet;
import com.finovara.finovarabackend.wallet.repository.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AddBalanceToPiggyBankTest {

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

    private final Long userId = 1L;
    private final Long piggyBankId = 1L;

    @BeforeEach
    void setUp(){
    }

    @Test
    void shouldAddBalanceSuccessfully() {
        User user = new User();

        Wallet wallet = new Wallet();
        wallet.setBalance(new BigDecimal("500"));

        PiggyBank piggyBank = new PiggyBank();
        piggyBank.setAmount(new BigDecimal("200"));
        piggyBank.setGoalAmount(new BigDecimal("1000"));

        when(userManagerService.getUserByIdOrThrow(userId)).thenReturn(user);
        when(piggyBankManagerService.getPiggyBankByUserId(piggyBankId, userId)).thenReturn(piggyBank);
        when(walletManagerService.getWalletByUserIdOrThrow(userId)).thenReturn(wallet);

        piggyBankTransactionService.addBalanceToPiggyBank(userId, piggyBankId, new BigDecimal("100"));

        assertEquals(new BigDecimal("400"), wallet.getBalance());
        assertEquals(new BigDecimal("300"), piggyBank.getAmount());

        verify(walletRepository).save(wallet);
        verify(piggyBankRepository).save(piggyBank);

        verify(piggyBankActivityService).createPaymentPiggyBankActivity(
                eq(userId),
                eq(piggyBank),
                eq(PiggyBankActivityType.AMOUNT_ADDED_TO_PIGGY_BANK_DIRECTLY),
                eq(new BigDecimal("100"))
        );
    }

    @Test
    void shouldCallGoalCompletionWhenGoalReached() {
        User user = new User();

        Wallet wallet = new Wallet();
        wallet.setBalance(new BigDecimal("500"));

        PiggyBank piggyBank = new PiggyBank();
        piggyBank.setAmount(new BigDecimal("900"));
        piggyBank.setGoalAmount(new BigDecimal("1000"));

        when(userManagerService.getUserByIdOrThrow(userId)).thenReturn(user);
        when(piggyBankManagerService.getPiggyBankByUserId(piggyBankId, userId)).thenReturn(piggyBank);
        when(walletManagerService.getWalletByUserIdOrThrow(userId)).thenReturn(wallet);

        piggyBankTransactionService.addBalanceToPiggyBank(userId, piggyBankId, new BigDecimal("100"));

        assertEquals(new BigDecimal("1000"), piggyBank.getAmount());

        verify(goalCompletionService).handleGoalCompletion(userId);
    }

    @Test
    void shouldThrowWhenInsufficientFunds() {

        User user = new User();

        Wallet wallet = new Wallet();
        wallet.setBalance(new BigDecimal("50"));

        PiggyBank piggyBank = new PiggyBank();
        piggyBank.setAmount(new BigDecimal("200"));

        when(userManagerService.getUserByIdOrThrow(userId)).thenReturn(user);
        when(piggyBankManagerService.getPiggyBankByUserId(piggyBankId, userId)).thenReturn(piggyBank);
        when(walletManagerService.getWalletByUserIdOrThrow(userId)).thenReturn(wallet);

        assertThrows(InvalidInputException.class, () -> piggyBankTransactionService.addBalanceToPiggyBank(userId, piggyBankId, new BigDecimal("100")));

        verify(piggyBankRepository, never()).save(any());
        verify(walletRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenUserNotFound() {
        when(userManagerService.getUserByIdOrThrow(userId))
                .thenThrow(new UserNotFoundException("User not found"));

        assertThrows(UserNotFoundException.class, () -> piggyBankTransactionService.addBalanceToPiggyBank(userId, piggyBankId, new BigDecimal("100")));

        verify(piggyBankRepository, never()).save(any());
        verify(walletRepository, never()).save(any());
    }
}