package com.finovara.finovarabackend.usersetting.piggybank.completion.service.handle;

import com.finovara.finovarabackend.accountactivity.piggybank.model.PiggyBankActivityType;
import com.finovara.finovarabackend.accountactivity.piggybank.service.PiggyBankActivityService;
import com.finovara.finovarabackend.piggybank.model.PiggyBank;
import com.finovara.finovarabackend.piggybank.repository.PiggyBankRepository;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.usersetting.piggybank.completion.model.GoalCompletionStrategy;
import com.finovara.finovarabackend.usersetting.piggybank.completion.service.GoalCompletionService;
import com.finovara.finovarabackend.usersetting.piggybank.model.PiggyBankSettings;
import com.finovara.finovarabackend.util.service.user.service.UserManagerService;
import com.finovara.finovarabackend.util.service.wallet.WalletManagerService;
import com.finovara.finovarabackend.wallet.model.Wallet;
import com.finovara.finovarabackend.wallet.repository.WalletRepository;
import com.finovara.finovarabackend.wallet.service.WalletService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HandleGoalCompletionTest {
    @Mock
    private UserManagerService userManagerService;
    @Mock
    private WalletManagerService walletManagerService;
    @Mock
    private PiggyBankRepository piggyBankRepository;
    @Mock
    private PiggyBankActivityService piggyBankActivityService;
    @InjectMocks
    private GoalCompletionService goalCompletionService;
    @Mock
    private WalletRepository walletRepository;

    private Wallet wallet;
    private PiggyBank piggyBank;

    private final String EMAIL = "test@test.com";

    @BeforeEach
    void setup() {
        User user = new User();
        user.setEmail(EMAIL);

        wallet = new Wallet();
        wallet.setBalance(BigDecimal.valueOf(500));

        piggyBank = new PiggyBank();
        piggyBank.setAmount(BigDecimal.valueOf(200));
        piggyBank.setGoalAmount(BigDecimal.valueOf(200));

        PiggyBankSettings settings = new PiggyBankSettings();
        settings.setGoalCompletionStrategy(GoalCompletionStrategy.NONE);
        piggyBank.setSettings(settings);

        user.setPiggyBanks(new ArrayList<>(List.of(piggyBank)));

        when(userManagerService.getUserByEmailOrThrow(EMAIL)).thenReturn(user);
        when(walletManagerService.getWalletByUserEmailOrThrow(EMAIL)).thenReturn(wallet);
        when(piggyBankRepository.findAllByUserAssignedEmail(EMAIL)).thenReturn(List.of(piggyBank));
    }

    @Test
    void shouldDoNothingWhenGoalNotCompleted() {
        piggyBank.setAmount(BigDecimal.valueOf(199));
        piggyBank.getSettings().setGoalCompletionStrategy(GoalCompletionStrategy.WITHDRAW_AND_KEEP);

        goalCompletionService.handleGoalCompletion(EMAIL);

        assertEquals(BigDecimal.valueOf(500), wallet.getBalance());
        assertEquals(BigDecimal.valueOf(199), piggyBank.getAmount());
        // Możesz dodać też weryfikację, że żadna aktywność nie została utworzona:
        verify(piggyBankActivityService, never()).createPaymentPiggyBankActivity(any(), any(), any(), any());
        verify(piggyBankActivityService, never()).createSimplePiggyBankActivity(any(), any(), any());
    }

    @Test
    void shouldDoNothingWhenStrategyNone() {
        piggyBank.getSettings().setGoalCompletionStrategy(GoalCompletionStrategy.NONE);
        piggyBank.setAmount(BigDecimal.valueOf(200));

        goalCompletionService.handleGoalCompletion(EMAIL);

        assertEquals(BigDecimal.valueOf(500), wallet.getBalance());
        assertEquals(BigDecimal.valueOf(200), piggyBank.getAmount());
        verify(piggyBankActivityService, never()).createPaymentPiggyBankActivity(any(), any(), any(), any());
    }

    @Test
    void shouldWithdrawAndKeepPiggyBank() {
        piggyBank.getSettings().setGoalCompletionStrategy(GoalCompletionStrategy.WITHDRAW_AND_KEEP);
        piggyBank.setAmount(BigDecimal.valueOf(200));

        goalCompletionService.handleGoalCompletion(EMAIL);

        assertEquals(BigDecimal.valueOf(700), wallet.getBalance());
        assertEquals(BigDecimal.ZERO, piggyBank.getAmount());

        verify(piggyBankActivityService).createPaymentPiggyBankActivity(
                eq(EMAIL),
                eq(piggyBank),
                eq(PiggyBankActivityType.AMOUNT_REMOVED_FROM_PIGGY_BANK_BY_SETTING),
                eq(BigDecimal.valueOf(200))
        );
    }

    @Test
    void shouldWithdrawAndDeletePiggyBank() {
        piggyBank.getSettings().setGoalCompletionStrategy(GoalCompletionStrategy.WITHDRAW_AND_DELETE);
        piggyBank.setAmount(BigDecimal.valueOf(200));

        goalCompletionService.handleGoalCompletion(EMAIL);

        assertEquals(BigDecimal.valueOf(700), wallet.getBalance());
        assertEquals(BigDecimal.ZERO, piggyBank.getAmount());

        verify(piggyBankActivityService).createPaymentPiggyBankActivity(any(), any(), any(), any());
        verify(piggyBankActivityService).createSimplePiggyBankActivity(
                eq(EMAIL),
                eq(piggyBank),
                eq(PiggyBankActivityType.DELETED_PIGGY_BANK)
        );
    }
}