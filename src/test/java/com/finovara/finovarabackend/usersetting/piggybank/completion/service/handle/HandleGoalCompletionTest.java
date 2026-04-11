package com.finovara.finovarabackend.usersetting.piggybank.completion.service.handle;

import com.finovara.finovarabackend.piggybank.model.PiggyBank;
import com.finovara.finovarabackend.piggybank.repository.PiggyBankRepository;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.usersetting.piggybank.completion.model.GoalCompletionStrategy;
import com.finovara.finovarabackend.usersetting.piggybank.completion.service.GoalCompletionCore;
import com.finovara.finovarabackend.usersetting.piggybank.completion.service.GoalCompletionService;
import com.finovara.finovarabackend.usersetting.piggybank.model.PiggyBankSettings;
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
import java.util.List;

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
    private GoalCompletionCore goalCompletionCore;

    @Mock
    private WalletRepository walletRepository;

    @InjectMocks
    private GoalCompletionService goalCompletionService;

    private static final String EMAIL = "test@test.com";

    private User user;
    private Wallet wallet;
    private PiggyBank piggyBank;
    private PiggyBankSettings settings;

    @BeforeEach
    void setup() {
        user = new User();
        user.setEmail(EMAIL);

        wallet = new Wallet();
        wallet.setBalance(BigDecimal.valueOf(500));

        piggyBank = new PiggyBank();
        piggyBank.setAmount(BigDecimal.valueOf(200));
        piggyBank.setGoalAmount(BigDecimal.valueOf(200));

        settings = new PiggyBankSettings();
        settings.setGoalCompletionStrategy(GoalCompletionStrategy.WITHDRAW_AND_KEEP);

        piggyBank.setSettings(settings);

        when(userManagerService.getUserByEmailOrThrow(EMAIL)).thenReturn(user);
        when(walletManagerService.getWalletByUserEmailOrThrow(EMAIL)).thenReturn(wallet);
        when(piggyBankRepository.findAllByUserAssignedEmail(EMAIL)).thenReturn(List.of(piggyBank));
    }

    @Test
    void shouldNotCallCoreWhenGoalNotReached() {
        piggyBank.setAmount(BigDecimal.valueOf(100));

        goalCompletionService.handleGoalCompletion(EMAIL);

        verify(goalCompletionCore, never()).apply(any(), any(), any(), any(), any());

        verify(walletRepository).save(wallet);
    }

    @Test
    void shouldCallCoreWhenGoalReached() {
        piggyBank.setAmount(BigDecimal.valueOf(200));

        goalCompletionService.handleGoalCompletion(EMAIL);

        verify(goalCompletionCore).apply(eq(EMAIL), eq(piggyBank), eq(wallet), eq(user), eq(GoalCompletionStrategy.WITHDRAW_AND_KEEP));

        verify(walletRepository).save(wallet);
    }

    @Test
    void shouldUseNoneWhenStrategyIsNull() {
        settings.setGoalCompletionStrategy(null);
        piggyBank.setAmount(BigDecimal.valueOf(200));

        goalCompletionService.handleGoalCompletion(EMAIL);

        verify(goalCompletionCore).apply(eq(EMAIL), eq(piggyBank), eq(wallet), eq(user), eq(GoalCompletionStrategy.NONE));
    }

    @Test
    void shouldHandleMultiplePiggyBanks() {
        PiggyBank second = new PiggyBank();
        second.setAmount(BigDecimal.valueOf(200));
        second.setGoalAmount(BigDecimal.valueOf(200));

        PiggyBankSettings secondSettings = new PiggyBankSettings();
        secondSettings.setGoalCompletionStrategy(GoalCompletionStrategy.NONE);
        second.setSettings(secondSettings);

        when(piggyBankRepository.findAllByUserAssignedEmail(EMAIL)).thenReturn(List.of(piggyBank, second));

        goalCompletionService.handleGoalCompletion(EMAIL);

        verify(goalCompletionCore, times(2)).apply(any(), any(), any(), any(), any());

        verify(walletRepository).save(wallet);
    }
}