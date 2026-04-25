package com.finovara.finovarabackend.usersetting.piggybank.autopayments.service;

import com.finovara.finovarabackend.accountactivity.settings.model.SettingActivityStatus;
import com.finovara.finovarabackend.accountactivity.settings.model.SettingType;
import com.finovara.finovarabackend.accountactivity.settings.service.SettingsActivityService;
import com.finovara.finovarabackend.piggybank.model.PiggyBank;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.usersetting.piggybank.autopayments.dto.AutoPaymentsDto;
import com.finovara.finovarabackend.usersetting.piggybank.autopayments.model.PiggyBankAutomationMode;
import com.finovara.finovarabackend.usersetting.piggybank.completion.service.GoalCompletionService;
import com.finovara.finovarabackend.usersetting.piggybank.model.PiggyBankSettings;
import com.finovara.finovarabackend.util.piggybank.manager.PiggyBankManagerService;
import com.finovara.finovarabackend.util.user.service.UserManagerService;
import com.finovara.finovarabackend.util.wallet.WalletManagerService;
import com.finovara.finovarabackend.wallet.model.Wallet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AutoPaymentsServiceTest {

    @Mock
    private UserManagerService userManagerService;
    @Mock
    private PiggyBankManagerService piggyBankManagerService;
    @Mock
    private WalletManagerService walletManagerService;
    @Mock
    private SettingsActivityService settingsActivityService;
    @Mock
    private AutoPaymentsCore autoPaymentsCore;
    @Mock
    private GoalCompletionService goalCompletionService;

    @InjectMocks
    private AutoPaymentsService autoPaymentsService;

    private final Long USER_ID = 1L;
    private final Long PIGGY_ID = 1L;

    private PiggyBank piggyBank;
    private Wallet wallet;
    private User user;

    @BeforeEach
    void setup() {
        wallet = new Wallet();
        user = new User();

        PiggyBankSettings settings = new PiggyBankSettings();
        piggyBank = new PiggyBank();
        piggyBank.setSettings(settings);

        when(userManagerService.getUserByIdOrThrow(USER_ID)).thenReturn(user);
        lenient().when(walletManagerService.getWalletByUserIdOrThrow(USER_ID)).thenReturn(wallet);
    }

    @Nested
    class CreateAndSaveAutoPayments {

        @Test
        void shouldActivateAutomationWithPercentage() {
            when(piggyBankManagerService.getPiggyBankByUserId(PIGGY_ID, USER_ID)).thenReturn(piggyBank);

            autoPaymentsService.createAutomation(USER_ID, PIGGY_ID, new AutoPaymentsDto(true, BigDecimal.valueOf(20)));

            assertTrue(piggyBank.getSettings().isAutomationActive());
            assertThat(piggyBank.getSettings().getAutomationPercentage()).isEqualByComparingTo(BigDecimal.valueOf(20));
        }

        @Test
        void shouldDeactivateAutomationAndResetPercentage() {
            when(piggyBankManagerService.getPiggyBankByUserId(PIGGY_ID, USER_ID)).thenReturn(piggyBank);

            autoPaymentsService.createAutomation(USER_ID, PIGGY_ID, new AutoPaymentsDto(false, null));

            assertFalse(piggyBank.getSettings().isAutomationActive());
            assertEquals(BigDecimal.ZERO, piggyBank.getSettings().getAutomationPercentage());
        }

        @Test
        void shouldSaveAutomationAndCreateActivity() {
            when(piggyBankManagerService.getPiggyBankByUserId(PIGGY_ID, USER_ID)).thenReturn(piggyBank);

            autoPaymentsService.saveAutoPaymentsPiggyBank(USER_ID, PIGGY_ID, new AutoPaymentsDto(true, BigDecimal.TEN));

            verify(settingsActivityService).createSettingActivity(USER_ID, SettingActivityStatus.ENABLED, SettingType.PIGGY_BANK_AUTO_PAYMENTS);
        }

        @Test
        void shouldThrowExceptionWhenActiveWithoutPercentage() {
            when(piggyBankManagerService.getPiggyBankByUserId(PIGGY_ID, USER_ID)).thenReturn(piggyBank);

            assertThrows(IllegalArgumentException.class, () -> autoPaymentsService.saveAutoPaymentsPiggyBank(USER_ID, PIGGY_ID, new AutoPaymentsDto(true, null)));
        }
    }

    @Nested
    class GetAutoPayments {

        @Test
        void shouldReturnAutomationSettings() {
            PiggyBankSettings settings = new PiggyBankSettings();
            settings.setAutomationActive(true);
            settings.setAutomationPercentage(BigDecimal.valueOf(15));
            piggyBank.setSettings(settings);

            when(piggyBankManagerService.getPiggyBankByUserId(PIGGY_ID, USER_ID)).thenReturn(piggyBank);

            AutoPaymentsDto result = autoPaymentsService.getAutomation(USER_ID, PIGGY_ID);

            assertTrue(result.isAutomationActive());
            assertEquals(BigDecimal.valueOf(15), result.percentage());
        }

        @Test
        void shouldReturnZeroWhenInactive() {
            PiggyBankSettings settings = new PiggyBankSettings();
            settings.setAutomationActive(false);
            settings.setAutomationPercentage(BigDecimal.ZERO);
            piggyBank.setSettings(settings);

            when(piggyBankManagerService.getPiggyBankByUserId(PIGGY_ID, USER_ID)).thenReturn(piggyBank);

            AutoPaymentsDto result = autoPaymentsService.getAutomation(USER_ID, PIGGY_ID);

            assertFalse(result.isAutomationActive());
            assertEquals(BigDecimal.ZERO, result.percentage());
        }
    }

    @Nested
    class HandleAutoPayments {

        @Test
        void shouldDoNothingWhenNoPiggyBanks() {
            user.setPiggyBanks(List.of());

            autoPaymentsService.handleRevenuePiggyBankAutomation(USER_ID, BigDecimal.TEN, PiggyBankAutomationMode.APPLY);

            verifyNoInteractions(autoPaymentsCore);
        }

        @Test
        void shouldSkipInactivePiggyBanks() {
            PiggyBankSettings settings = new PiggyBankSettings();
            settings.setAutomationActive(false);

            PiggyBank piggy = new PiggyBank();
            piggy.setSettings(settings);

            user.setPiggyBanks(List.of(piggy));

            autoPaymentsService.handleRevenuePiggyBankAutomation(USER_ID, BigDecimal.TEN, PiggyBankAutomationMode.APPLY);

            verifyNoInteractions(autoPaymentsCore);
            verify(goalCompletionService).handleGoalCompletion(USER_ID);
        }

        @ParameterizedTest
        @EnumSource(PiggyBankAutomationMode.class)
        void shouldProcessActivePiggyBank(PiggyBankAutomationMode mode) {
            PiggyBankSettings settings = new PiggyBankSettings();
            settings.setAutomationActive(true);
            settings.setAutomationPercentage(BigDecimal.TEN);

            PiggyBank piggy = new PiggyBank();
            piggy.setSettings(settings);

            user.setPiggyBanks(List.of(piggy));

            when(walletManagerService.getWalletByUserIdOrThrow(USER_ID)).thenReturn(wallet);

            autoPaymentsService.handleRevenuePiggyBankAutomation(USER_ID, BigDecimal.TEN, mode);

            verify(autoPaymentsCore).process(eq(USER_ID), eq(piggy), eq(wallet), any(), eq(mode));

            verify(goalCompletionService).handleGoalCompletion(USER_ID);
        }
    }
}