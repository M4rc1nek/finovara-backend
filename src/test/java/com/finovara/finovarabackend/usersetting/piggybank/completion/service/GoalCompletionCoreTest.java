package com.finovara.finovarabackend.usersetting.piggybank.completion.service;

import com.finovara.finovarabackend.accountactivity.piggybank.model.PiggyBankActivityType;
import com.finovara.finovarabackend.accountactivity.piggybank.service.PiggyBankActivityService;
import com.finovara.finovarabackend.exception.badrequest.InvalidInputException;
import com.finovara.finovarabackend.piggybank.model.PiggyBank;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.usersetting.piggybank.completion.model.GoalCompletionStrategy;
import com.finovara.finovarabackend.wallet.model.Wallet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GoalCompletionCoreTest {

    @Mock
    private PiggyBankActivityService piggyBankActivityService;

    @InjectMocks
    private GoalCompletionCore goalCompletionCore;

    private PiggyBank piggyBank;
    private Wallet wallet;
    private User user;

    private static final Long USER_ID = 1L;

    @BeforeEach
    void setup() {
        piggyBank = new PiggyBank();
        piggyBank.setAmount(BigDecimal.valueOf(200));

        wallet = new Wallet();
        wallet.setBalance(BigDecimal.valueOf(500));

        user = new User();
        user.setPiggyBanks(new ArrayList<>());
        user.getPiggyBanks().add(piggyBank);
    }

    @Test
    void shouldDoNothingForNoneStrategy() {
        goalCompletionCore.apply(USER_ID, piggyBank, wallet, user, GoalCompletionStrategy.NONE);

        assertEquals(BigDecimal.valueOf(500), wallet.getBalance());
        assertEquals(BigDecimal.valueOf(200), piggyBank.getAmount());

        verifyNoInteractions(piggyBankActivityService);
    }

    @Test
    void shouldTransferMoneyAndKeepPiggyBank() {
        goalCompletionCore.apply(USER_ID, piggyBank, wallet, user, GoalCompletionStrategy.WITHDRAW_AND_KEEP);

        assertEquals(BigDecimal.valueOf(700), wallet.getBalance());
        assertEquals(BigDecimal.ZERO, piggyBank.getAmount());

        verify(piggyBankActivityService).createPaymentPiggyBankActivity(eq(USER_ID), eq(piggyBank), eq(PiggyBankActivityType.AMOUNT_REMOVED_FROM_PIGGY_BANK_BY_SETTING), eq(BigDecimal.valueOf(200)));

        verify(piggyBankActivityService, never()).createSimplePiggyBankActivity(anyLong(), any(), any());
        assertTrue(user.getPiggyBanks().contains(piggyBank));
    }

    @Test
    void shouldTransferMoneyAndDeletePiggyBank() {
        goalCompletionCore.apply(USER_ID, piggyBank, wallet, user, GoalCompletionStrategy.WITHDRAW_AND_DELETE);

        assertEquals(BigDecimal.valueOf(700), wallet.getBalance());
        assertEquals(BigDecimal.ZERO, piggyBank.getAmount());

        verify(piggyBankActivityService).createPaymentPiggyBankActivity(anyLong(), any(), any(), any());
        verify(piggyBankActivityService).createSimplePiggyBankActivity(eq(USER_ID), eq(piggyBank), eq(PiggyBankActivityType.DELETED_PIGGY_BANK));

        assertFalse(user.getPiggyBanks().contains(piggyBank));
    }

    @Test
    void shouldNotTransferWhenAmountIsZero() {
        piggyBank.setAmount(BigDecimal.ZERO);

        goalCompletionCore.apply(USER_ID, piggyBank, wallet, user, GoalCompletionStrategy.WITHDRAW_AND_KEEP);

        assertEquals(BigDecimal.valueOf(500), wallet.getBalance());
        assertEquals(BigDecimal.ZERO, piggyBank.getAmount());

        verify(piggyBankActivityService).createPaymentPiggyBankActivity(eq(USER_ID), eq(piggyBank), eq(PiggyBankActivityType.AMOUNT_REMOVED_FROM_PIGGY_BANK_BY_SETTING), eq(BigDecimal.ZERO));
    }

    @Test
    void shouldNotTransferWhenAmountIsNull() {
        piggyBank.setAmount(null);

        goalCompletionCore.apply(USER_ID, piggyBank, wallet, user, GoalCompletionStrategy.WITHDRAW_AND_KEEP);

        assertEquals(BigDecimal.valueOf(500), wallet.getBalance());
        assertNull(piggyBank.getAmount());

        verify(piggyBankActivityService).createPaymentPiggyBankActivity(eq(USER_ID), eq(piggyBank), eq(PiggyBankActivityType.AMOUNT_REMOVED_FROM_PIGGY_BANK_BY_SETTING), isNull());
    }

    @Test
    void shouldThrowWhenDeleteAndMoneyStillExists() {
        piggyBank.setAmount(BigDecimal.valueOf(200));

        doAnswer(invocation -> {
            piggyBank.setAmount(BigDecimal.valueOf(100));
            return null;
        }).when(piggyBankActivityService).createSimplePiggyBankActivity(anyLong(), any(), any());

        assertThrows(InvalidInputException.class, () -> goalCompletionCore.apply(USER_ID, piggyBank, wallet, user, GoalCompletionStrategy.WITHDRAW_AND_DELETE));
    }
}