package com.finovara.finovarabackend.usersetting.finances.revenue.scoring.service.save;

import com.finovara.finovarabackend.accountactivity.settings.model.SettingActivityStatus;
import com.finovara.finovarabackend.accountactivity.settings.model.SettingType;
import com.finovara.finovarabackend.accountactivity.settings.service.SettingsActivityService;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.usersetting.finances.revenue.model.RevenueSettings;
import com.finovara.finovarabackend.usersetting.finances.revenue.scoring.dto.RevenueScoringDto;
import com.finovara.finovarabackend.usersetting.finances.revenue.scoring.service.RevenueScoringService;
import com.finovara.finovarabackend.util.service.user.service.UserManagerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class SaveScoringIncomeTest {

    @Mock
    private UserManagerService userManagerService;

    @Mock
    private SettingsActivityService settingsActivityService;

    @InjectMocks
    private RevenueScoringService revenueScoringService;

    private RevenueSettings revenueSettings;
    private final String EMAIL = "test@test.com";

    @BeforeEach
    void setup() {
        User user = new User();
        revenueSettings = new RevenueSettings();
        user.setRevenueSettings(revenueSettings);

        when(userManagerService.getUserByEmailOrThrow(EMAIL)).thenReturn(user);
    }

    @Test
    void shouldEnableScoring() {
        RevenueScoringDto dto = new RevenueScoringDto(true, null);

        revenueScoringService.saveScoringIncome(EMAIL, dto);

        assertTrue(revenueSettings.isScoringEnable());
        verify(settingsActivityService).createSettingActivity(EMAIL, SettingActivityStatus.ENABLED, SettingType.REVENUE_SCORING);
    }

    @Test
    void shouldDisableScoring() {
        RevenueScoringDto dto = new RevenueScoringDto(false, null);

        revenueScoringService.saveScoringIncome(EMAIL, dto);

        assertFalse(revenueSettings.isScoringEnable());
        verify(settingsActivityService).createSettingActivity(EMAIL, SettingActivityStatus.DISABLED, SettingType.REVENUE_SCORING);
    }
}