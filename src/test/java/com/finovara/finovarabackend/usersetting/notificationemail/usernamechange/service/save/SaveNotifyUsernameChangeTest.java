package com.finovara.finovarabackend.usersetting.notificationemail.usernamechange.service.save;

import com.finovara.finovarabackend.accountactivity.settings.model.SettingActivityStatus;
import com.finovara.finovarabackend.accountactivity.settings.service.SettingsActivityService;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.usersetting.notificationemail.model.NotificationEmailSettings;
import com.finovara.finovarabackend.usersetting.notificationemail.usernamechange.dto.NotifyUsernameChangeDto;
import com.finovara.finovarabackend.usersetting.notificationemail.usernamechange.service.NotifyUsernameChangeService;
import com.finovara.finovarabackend.util.user.service.UserManagerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.finovara.finovarabackend.accountactivity.settings.model.SettingType.NOTIFICATION_USERNAME_CHANGED;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SaveNotifyUsernameChangeTest {

    @Mock
    private UserManagerService userManagerService;

    @Mock
    private SettingsActivityService settingsActivityService;

    @InjectMocks
    private NotifyUsernameChangeService notifyUsernameChangeService;

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
    void shouldEnableNotifyOnUsernameChange() {
        NotifyUsernameChangeDto dto = new NotifyUsernameChangeDto(true);

        notifyUsernameChangeService.saveNotifyUsernameChange(EMAIL, dto);

        assertTrue(notificationEmailSettings.isNotifyOnUsernameChange());
        verify(settingsActivityService).createSettingActivity(EMAIL, SettingActivityStatus.ENABLED, NOTIFICATION_USERNAME_CHANGED);
    }

    @Test
    void shouldDisableNotifyOnUsernameChange() {
        notificationEmailSettings.setNotifyOnUsernameChange(true);

        NotifyUsernameChangeDto dto = new NotifyUsernameChangeDto(false);

        notifyUsernameChangeService.saveNotifyUsernameChange(EMAIL, dto);

        assertFalse(notificationEmailSettings.isNotifyOnUsernameChange());
        verify(settingsActivityService).createSettingActivity(EMAIL, SettingActivityStatus.DISABLED, NOTIFICATION_USERNAME_CHANGED);
    }
}