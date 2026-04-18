package com.finovara.finovarabackend.usersetting.piggybank.rondup.service.save;

import com.finovara.finovarabackend.accountactivity.settings.model.SettingActivityStatus;
import com.finovara.finovarabackend.accountactivity.settings.model.SettingType;
import com.finovara.finovarabackend.accountactivity.settings.service.SettingsActivityService;
import com.finovara.finovarabackend.piggybank.model.PiggyBank;
import com.finovara.finovarabackend.usersetting.piggybank.model.PiggyBankSettings;
import com.finovara.finovarabackend.usersetting.piggybank.roundup.dto.RoundUpDto;
import com.finovara.finovarabackend.usersetting.piggybank.roundup.service.RoundUpService;
import com.finovara.finovarabackend.util.piggybank.manager.PiggyBankManagerService;
import com.finovara.finovarabackend.util.user.service.UserManagerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SaveRoundUpTest {

    @Mock
    private UserManagerService userManagerService;
    @Mock
    private PiggyBankManagerService piggyBankManagerService;
    @Mock
    private SettingsActivityService settingsActivityService;

    @InjectMocks
    private RoundUpService roundUpService;

    private PiggyBank piggyBank;
    private final Long userId = 1L;
    private final Long piggyBankId = 1L;

    @BeforeEach
    void setup() {
        piggyBank = new PiggyBank();
        PiggyBankSettings settings = new PiggyBankSettings();
        piggyBank.setSettings(settings);

        when(piggyBankManagerService.getPiggyBankByUserId(piggyBankId, userId)).thenReturn(piggyBank);
        when(userManagerService.getUserByIdOrThrow(userId)).thenReturn(null);
    }

    @Test
    void shouldSaveRoundUpActiveEnabled() {
        RoundUpDto dto = new RoundUpDto(true);

        roundUpService.saveRoundUpPiggyBank(userId, piggyBankId, dto);

        assert(piggyBank.getSettings().isRoundUpActive());
        verify(settingsActivityService).createSettingActivity(userId, SettingActivityStatus.ENABLED, SettingType.PIGGY_BANK_ROUND_UP);
    }

    @Test
    void shouldSaveRoundUpActiveDisabled() {
        RoundUpDto dto = new RoundUpDto(false);

        roundUpService.saveRoundUpPiggyBank(userId, piggyBankId, dto);

        assert(!piggyBank.getSettings().isRoundUpActive());
        verify(settingsActivityService).createSettingActivity(userId, SettingActivityStatus.DISABLED, SettingType.PIGGY_BANK_ROUND_UP);
    }
}