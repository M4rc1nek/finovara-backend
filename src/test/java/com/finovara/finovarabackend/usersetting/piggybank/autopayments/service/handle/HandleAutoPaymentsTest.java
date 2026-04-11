package com.finovara.finovarabackend.usersetting.piggybank.autopayments.service.handle;

import com.finovara.finovarabackend.piggybank.model.PiggyBank;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.usersetting.piggybank.autopayments.model.PiggyBankAutomationMode;
import com.finovara.finovarabackend.usersetting.piggybank.autopayments.service.AutoPaymentsCore;
import com.finovara.finovarabackend.usersetting.piggybank.autopayments.service.AutoPaymentsService;
import com.finovara.finovarabackend.usersetting.piggybank.completion.service.GoalCompletionService;
import com.finovara.finovarabackend.usersetting.piggybank.model.PiggyBankSettings;
import com.finovara.finovarabackend.util.user.service.UserManagerService;
import com.finovara.finovarabackend.util.wallet.WalletManagerService;
import com.finovara.finovarabackend.wallet.model.Wallet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HandleAutoPaymentsTest {

    @Mock
    private UserManagerService userManagerService;
    @Mock
    private WalletManagerService walletManagerService;
    @Mock
    private GoalCompletionService goalCompletionService;
    @Mock
    private AutoPaymentsCore autoPaymentsCore;

    @InjectMocks
    private AutoPaymentsService autoPaymentsService;

    private String email;
    private Wallet wallet;

    @BeforeEach
    void setUp() {
        email = "test@mail.com";
        wallet = new Wallet();

        when(walletManagerService.getWalletByUserEmailOrThrow(email)).thenReturn(wallet);
    }

    @Test
    void shouldDoNothingWhenUserHasNoPiggyBanks() {
        User user = new User();
        user.setPiggyBanks(List.of());

        when(userManagerService.getUserByEmailOrThrow(email)).thenReturn(user);

        autoPaymentsService.handleRevenuePiggyBankAutomation(email, BigDecimal.TEN, PiggyBankAutomationMode.APPLY);

        verifyNoInteractions(autoPaymentsCore);
        verifyNoInteractions(goalCompletionService);
    }

    @Test
    void shouldHandleNullPiggyBanks() {
        User user = new User();
        user.setPiggyBanks(null);

        when(userManagerService.getUserByEmailOrThrow(email)).thenReturn(user);

        autoPaymentsService.handleRevenuePiggyBankAutomation(email, BigDecimal.TEN, PiggyBankAutomationMode.APPLY);

        verifyNoInteractions(autoPaymentsCore);
        verifyNoInteractions(goalCompletionService);
    }

    @Test
    void shouldSkipInactivePiggyBanks() {
        PiggyBankSettings settings = new PiggyBankSettings();
        settings.setAutomationActive(false);

        PiggyBank piggyBank = new PiggyBank();
        piggyBank.setSettings(settings);

        User user = new User();
        user.setPiggyBanks(List.of(piggyBank));

        when(userManagerService.getUserByEmailOrThrow(email)).thenReturn(user);

        autoPaymentsService.handleRevenuePiggyBankAutomation(email, BigDecimal.TEN, PiggyBankAutomationMode.APPLY);

        verifyNoInteractions(autoPaymentsCore);
        verify(goalCompletionService).handleGoalCompletion(email);
    }

    @Test
    void shouldHandleMultiplePiggyBanks_onlyActiveProcessed() {
        PiggyBankSettings activeSettings = new PiggyBankSettings();
        activeSettings.setAutomationActive(true);
        activeSettings.setAutomationPercentage(BigDecimal.TEN);

        PiggyBankSettings inactiveSettings = new PiggyBankSettings();
        inactiveSettings.setAutomationActive(false);

        PiggyBank activePiggyBank = new PiggyBank();
        activePiggyBank.setSettings(activeSettings);

        PiggyBank inactivePiggyBank = new PiggyBank();
        inactivePiggyBank.setSettings(inactiveSettings);

        User user = new User();
        user.setPiggyBanks(List.of(activePiggyBank, inactivePiggyBank));

        when(userManagerService.getUserByEmailOrThrow(email)).thenReturn(user);

        autoPaymentsService.handleRevenuePiggyBankAutomation(email, BigDecimal.TEN, PiggyBankAutomationMode.APPLY);

        verify(autoPaymentsCore).getCalculationCore(eq(email), eq(activePiggyBank), eq(wallet), any(), eq(PiggyBankAutomationMode.APPLY));

        verify(autoPaymentsCore, never()).getCalculationCore(eq(email), eq(inactivePiggyBank), any(), any(), any());

        verify(goalCompletionService).handleGoalCompletion(email);
    }

    @ParameterizedTest
    @EnumSource(PiggyBankAutomationMode.class)
    void shouldCallCoreForActivePiggyBank(PiggyBankAutomationMode mode) {
        PiggyBankSettings settings = new PiggyBankSettings();
        settings.setAutomationActive(true);
        settings.setAutomationPercentage(BigDecimal.TEN); // 10%

        PiggyBank piggyBank = new PiggyBank();
        piggyBank.setSettings(settings);

        User user = new User();
        user.setPiggyBanks(List.of(piggyBank));

        when(userManagerService.getUserByEmailOrThrow(email)).thenReturn(user);

        autoPaymentsService.handleRevenuePiggyBankAutomation(email, BigDecimal.TEN, mode);

        verify(autoPaymentsCore).getCalculationCore(eq(email), eq(piggyBank), eq(wallet), argThat(amount -> amount.compareTo(new BigDecimal("1.00")) == 0), eq(mode));

        verify(goalCompletionService).handleGoalCompletion(email);
    }
}