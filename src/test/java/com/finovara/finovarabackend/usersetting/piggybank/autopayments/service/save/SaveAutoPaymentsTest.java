package com.finovara.finovarabackend.usersetting.piggybank.autopayments.service.save;

import com.finovara.finovarabackend.accountactivity.settings.model.SettingActivityStatus;
import com.finovara.finovarabackend.accountactivity.settings.model.SettingType;
import com.finovara.finovarabackend.accountactivity.settings.service.SettingsActivityService;
import com.finovara.finovarabackend.piggybank.model.PiggyBank;
import com.finovara.finovarabackend.usersetting.piggybank.autopayments.dto.AutoPaymentsDto;
import com.finovara.finovarabackend.usersetting.piggybank.autopayments.service.AutoPaymentsService;
import com.finovara.finovarabackend.usersetting.piggybank.model.PiggyBankSettings;
import com.finovara.finovarabackend.util.piggybank.manager.PiggyBankManagerService;
import com.finovara.finovarabackend.util.user.service.UserManagerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SaveAutoPaymentsTest {

    @Mock
    private UserManagerService userManagerService;

    @Mock
    private PiggyBankManagerService piggyBankManagerService;

    @Mock
    private SettingsActivityService settingsActivityService;

    @InjectMocks
    private AutoPaymentsService autoPaymentsService;

    private final Long USER_ID = 1L;
    private PiggyBank piggyBank;

    @BeforeEach
    void setup() {
        piggyBank = new PiggyBank();
        PiggyBankSettings settings = new PiggyBankSettings();
        piggyBank.setSettings(settings);

        when(userManagerService.getUserByIdOrThrow(USER_ID)).thenReturn(new com.finovara.finovarabackend.user.model.User());
        when(piggyBankManagerService.getPiggyBankByUserId(1L, USER_ID)).thenReturn(piggyBank);
    }

    @Test
    void shouldSaveActiveAutomationWithPercentage() {
        AutoPaymentsDto dto = new AutoPaymentsDto(true, BigDecimal.valueOf(20));

        autoPaymentsService.saveAutoPaymentsPiggyBank(USER_ID, 1L, dto);

        assertTrue(piggyBank.getSettings().isAutomationActive());
        assertEquals(BigDecimal.valueOf(20), piggyBank.getSettings().getAutomationPercentage());
        verify(settingsActivityService).createSettingActivity(USER_ID, SettingActivityStatus.ENABLED, SettingType.PIGGY_BANK_AUTO_PAYMENTS);
    }

    @Test
    void shouldSaveInactiveAutomationWithZeroPercentage() {
        AutoPaymentsDto dto = new AutoPaymentsDto(false, null);

        autoPaymentsService.saveAutoPaymentsPiggyBank(USER_ID, 1L, dto);

        assertFalse(piggyBank.getSettings().isAutomationActive());
        assertEquals(BigDecimal.ZERO, piggyBank.getSettings().getAutomationPercentage());
        verify(settingsActivityService).createSettingActivity(USER_ID, SettingActivityStatus.DISABLED, SettingType.PIGGY_BANK_AUTO_PAYMENTS);
    }

    @Test
    void shouldThrowExceptionWhenActiveWithoutPercentage() {
        AutoPaymentsDto dto = new AutoPaymentsDto(true, null);

        assertThrows(IllegalArgumentException.class, () -> autoPaymentsService.saveAutoPaymentsPiggyBank(USER_ID, 1L, dto));
    }
}