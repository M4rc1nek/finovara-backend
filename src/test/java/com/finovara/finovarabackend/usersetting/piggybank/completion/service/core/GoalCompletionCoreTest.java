package com.finovara.finovarabackend.usersetting.piggybank.completion.service.core;

import com.finovara.finovarabackend.accountactivity.piggybank.model.PiggyBankActivityType;
import com.finovara.finovarabackend.accountactivity.piggybank.service.PiggyBankActivityService;
import com.finovara.finovarabackend.exception.badrequest.InvalidInputException;
import com.finovara.finovarabackend.piggybank.model.PiggyBank;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.usersetting.piggybank.completion.model.GoalCompletionStrategy;
import com.finovara.finovarabackend.usersetting.piggybank.completion.service.GoalCompletionCore;
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

    private final String EMAIL = "test@test.com";

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
        goalCompletionCore.apply(EMAIL, piggyBank, wallet, user, GoalCompletionStrategy.NONE);

        assertEquals(BigDecimal.valueOf(500), wallet.getBalance());
        assertEquals(BigDecimal.valueOf(200), piggyBank.getAmount());

        verifyNoInteractions(piggyBankActivityService);
    }

    @Test
    void shouldTransferMoneyAndKeepPiggyBank() {
        goalCompletionCore.apply(EMAIL, piggyBank, wallet, user, GoalCompletionStrategy.WITHDRAW_AND_KEEP);

        assertEquals(BigDecimal.valueOf(700), wallet.getBalance());
        assertEquals(BigDecimal.ZERO, piggyBank.getAmount());

        verify(piggyBankActivityService).createPaymentPiggyBankActivity(eq(EMAIL), eq(piggyBank), eq(PiggyBankActivityType.AMOUNT_REMOVED_FROM_PIGGY_BANK_BY_SETTING), eq(BigDecimal.valueOf(200)));

        verify(piggyBankActivityService, never()).createSimplePiggyBankActivity(any(), any(), any());
        assertTrue(user.getPiggyBanks().contains(piggyBank));
    }

    @Test
    void shouldTransferMoneyAndDeletePiggyBank() {
        goalCompletionCore.apply(EMAIL, piggyBank, wallet, user, GoalCompletionStrategy.WITHDRAW_AND_DELETE);

        assertEquals(BigDecimal.valueOf(700), wallet.getBalance());
        assertEquals(BigDecimal.ZERO, piggyBank.getAmount());

        verify(piggyBankActivityService).createPaymentPiggyBankActivity(any(), any(), any(), any());
        verify(piggyBankActivityService).createSimplePiggyBankActivity(eq(EMAIL), eq(piggyBank), eq(PiggyBankActivityType.DELETED_PIGGY_BANK));

        assertFalse(user.getPiggyBanks().contains(piggyBank));
    }

    @Test
    void shouldNotTransferWhenAmountIsZero() {
        piggyBank.setAmount(BigDecimal.ZERO);

        goalCompletionCore.apply(EMAIL, piggyBank, wallet, user, GoalCompletionStrategy.WITHDRAW_AND_KEEP);

        assertEquals(BigDecimal.valueOf(500), wallet.getBalance());
        assertEquals(BigDecimal.ZERO, piggyBank.getAmount());

        verify(piggyBankActivityService).createPaymentPiggyBankActivity(eq(EMAIL), eq(piggyBank), eq(PiggyBankActivityType.AMOUNT_REMOVED_FROM_PIGGY_BANK_BY_SETTING), eq(BigDecimal.ZERO));
    }

    @Test
    void shouldNotTransferWhenAmountIsNull() {
        piggyBank.setAmount(null);

        goalCompletionCore.apply(EMAIL, piggyBank, wallet, user, GoalCompletionStrategy.WITHDRAW_AND_KEEP);

        assertEquals(BigDecimal.valueOf(500), wallet.getBalance());
        assertNull(piggyBank.getAmount());

        verify(piggyBankActivityService).createPaymentPiggyBankActivity(eq(EMAIL), eq(piggyBank), eq(PiggyBankActivityType.AMOUNT_REMOVED_FROM_PIGGY_BANK_BY_SETTING), isNull());
    }

    @Test
    void shouldThrowWhenDeleteAndMoneyStillExists() {
        piggyBank.setAmount(BigDecimal.valueOf(200));

        doAnswer(invocation -> {
            piggyBank.setAmount(BigDecimal.valueOf(100));
            return null;
        }).when(piggyBankActivityService).createSimplePiggyBankActivity(any(), any(), any());

        assertThrows(InvalidInputException.class, () -> goalCompletionCore.apply(EMAIL, piggyBank, wallet, user, GoalCompletionStrategy.WITHDRAW_AND_DELETE));
    }
}