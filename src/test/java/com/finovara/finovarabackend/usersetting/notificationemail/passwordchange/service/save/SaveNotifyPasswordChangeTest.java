package com.finovara.finovarabackend.usersetting.notificationemail.passwordchange.service.save;

import com.finovara.finovarabackend.accountactivity.settings.model.SettingActivityStatus;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.usersetting.notificationemail.model.NotificationEmailSettings;
import com.finovara.finovarabackend.usersetting.notificationemail.passwordchange.dto.NotifyPasswordChangeDto;
import com.finovara.finovarabackend.usersetting.notificationemail.passwordchange.service.NotifyPasswordChangeService;
import com.finovara.finovarabackend.accountactivity.settings.service.SettingsActivityService;
import com.finovara.finovarabackend.util.user.service.UserManagerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.finovara.finovarabackend.accountactivity.settings.model.SettingType.NOTIFICATION_PASSWORD_CHANGED;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

@ExtendWith(MockitoExtension.class)
class SaveNotifyPasswordChangeTest {

    @Mock
    private UserManagerService userManagerService;

    @Mock
    private SettingsActivityService settingsActivityService;

    @InjectMocks
    private NotifyPasswordChangeService notifyPasswordChangeService;

    private NotificationEmailSettings notificationEmailSettings;
    private final String EMAIL = "test@test.com";

    @BeforeEach
    void setup() {
        User user = new User();
        notificationEmailSettings = new NotificationEmailSettings();
        user.setNotificationEmailSettings(notificationEmailSettings);

        when(userManagerService.getUserByEmailOrThrow(EMAIL)).thenReturn(user);
    }

    @Test
    void shouldEnableNotifyOnPasswordChange() {
        NotifyPasswordChangeDto dto = new NotifyPasswordChangeDto(true);

        notifyPasswordChangeService.saveNotifyOnPasswordChange(EMAIL, dto);

        assertTrue(notificationEmailSettings.isNotifyOnPasswordChange());
        verify(settingsActivityService).createSettingActivity(EMAIL, SettingActivityStatus.ENABLED, NOTIFICATION_PASSWORD_CHANGED);
    }

    @Test
    void shouldDisableNotifyOnPasswordChange() {
        notificationEmailSettings.setNotifyOnPasswordChange(true);

        NotifyPasswordChangeDto dto = new NotifyPasswordChangeDto(false);

        notifyPasswordChangeService.saveNotifyOnPasswordChange(EMAIL, dto);

        assertFalse(notificationEmailSettings.isNotifyOnPasswordChange());
        verify(settingsActivityService).createSettingActivity(EMAIL, SettingActivityStatus.DISABLED, NOTIFICATION_PASSWORD_CHANGED);
    }
}