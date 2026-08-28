package com.finovara.financeservice.settings.piggybank.autopayments.service;

import com.finovara.contracts.authorization.additionalcode.resolver.AdditionalAuthorizationCodeResolver;
import com.finovara.contracts.model.activity.SettingActivityStatus;
import com.finovara.contracts.activity.event.settings.SettingsActivityEvent;
import com.finovara.financeservice.piggybank.model.PiggyBank;
import com.finovara.financeservice.piggybank.repository.PiggyBankRepository;
import com.finovara.financeservice.settings.piggybank.autopayments.dto.AutoPaymentsDto;
import com.finovara.financeservice.settings.piggybank.autopayments.model.PiggyBankAutomationMode;
import com.finovara.financeservice.settings.piggybank.completion.service.GoalCompletionService;
import com.finovara.financeservice.settings.piggybank.model.PiggyBankSettings;
import com.finovara.financeservice.feignclient.AuthBackendClient;
import com.finovara.financeservice.util.transaction.piggybank.manager.PiggyBankManagerService;
import com.finovara.financeservice.util.wallet.WalletManagerService;
import com.finovara.financeservice.wallet.model.Wallet;
import org.springframework.kafka.core.KafkaTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AutoPaymentsServiceTest {

    @Mock
    private PiggyBankManagerService piggyBankManagerService;
    @Mock
    private PiggyBankRepository piggyBankRepository;
    @Mock
    private WalletManagerService walletManagerService;
    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;
    @Mock
    private AutoPaymentsCore autoPaymentsCore;
    @Mock
    private GoalCompletionService goalCompletionService;

    @Mock
    private AuthBackendClient authBackendClient;

    @Mock
    private AdditionalAuthorizationCodeResolver additionalAuthorizationCodeResolver;

    @InjectMocks
    private AutoPaymentsService autoPaymentsService;

    private final Long USER_ID = 1L;
    private final Long PIGGY_ID = 1L;

    private PiggyBank piggyBank;
    private Wallet wallet;

    @BeforeEach
    void setup() {
        wallet = Wallet.create(USER_ID);

        PiggyBankSettings settings = new PiggyBankSettings();
        piggyBank = new PiggyBank();
        piggyBank.setSettings(settings);
    }

    @Nested
    class CreateAndSaveAutoPayments {

        @Test
        void shouldActivateAutomationWithPercentage() {
            when(piggyBankManagerService.getPiggyBankByUserId(PIGGY_ID, USER_ID)).thenReturn(piggyBank);

            autoPaymentsService.createAutomation(USER_ID, PIGGY_ID, new AutoPaymentsDto(true, BigDecimal.valueOf(20), null));

            assertTrue(piggyBank.getSettings().isAutomationActive());
            assertThat(piggyBank.getSettings().getAutomationPercentage()).isEqualByComparingTo(BigDecimal.valueOf(20));
        }

        @Test
        void shouldDeactivateAutomationAndResetPercentage() {
            when(piggyBankManagerService.getPiggyBankByUserId(PIGGY_ID, USER_ID)).thenReturn(piggyBank);

            autoPaymentsService.createAutomation(USER_ID, PIGGY_ID, new AutoPaymentsDto(false, null, null));

            assertFalse(piggyBank.getSettings().isAutomationActive());
            assertEquals(BigDecimal.ZERO, piggyBank.getSettings().getAutomationPercentage());
        }

        @Test
        void shouldSaveAutomationAndCreateActivity() {
            when(piggyBankManagerService.getPiggyBankByUserId(PIGGY_ID, USER_ID)).thenReturn(piggyBank);

            autoPaymentsService.saveAutoPaymentsPiggyBank(USER_ID, PIGGY_ID, new AutoPaymentsDto(true, BigDecimal.TEN, null));

            ArgumentCaptor<SettingsActivityEvent> eventCaptor = ArgumentCaptor.forClass(SettingsActivityEvent.class);
            verify(kafkaTemplate).send(eq("activity.settings"), eventCaptor.capture());
            assertThat(eventCaptor.getValue().status()).isEqualTo(SettingActivityStatus.ENABLED);
        }

        @Test
        void shouldThrowExceptionWhenActiveWithoutPercentage() {
            when(piggyBankManagerService.getPiggyBankByUserId(PIGGY_ID, USER_ID)).thenReturn(piggyBank);

            assertThrows(IllegalArgumentException.class, () -> autoPaymentsService.saveAutoPaymentsPiggyBank(USER_ID, PIGGY_ID, new AutoPaymentsDto(true, null, null)));
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
            when(piggyBankRepository.findAllByUserId(USER_ID)).thenReturn(Collections.emptyList());

            autoPaymentsService.handleRevenuePiggyBankAutomation(USER_ID, BigDecimal.TEN, PiggyBankAutomationMode.APPLY);

            verifyNoInteractions(autoPaymentsCore);
        }

        @Test
        void shouldSkipInactivePiggyBanks() {
            PiggyBankSettings settings = new PiggyBankSettings();
            settings.setAutomationActive(false);

            PiggyBank piggy = new PiggyBank();
            piggy.setSettings(settings);

            when(piggyBankRepository.findAllByUserId(USER_ID)).thenReturn(List.of(piggy));
            when(walletManagerService.getWalletByUserIdOrThrow(USER_ID)).thenReturn(wallet);

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

            when(piggyBankRepository.findAllByUserId(USER_ID)).thenReturn(List.of(piggy));
            when(walletManagerService.getWalletByUserIdOrThrow(USER_ID)).thenReturn(wallet);

            autoPaymentsService.handleRevenuePiggyBankAutomation(USER_ID, BigDecimal.TEN, mode);

            verify(autoPaymentsCore).process(eq(USER_ID), eq(piggy), eq(wallet), any(), eq(mode));
            verify(goalCompletionService).handleGoalCompletion(USER_ID);
        }
    }
}
