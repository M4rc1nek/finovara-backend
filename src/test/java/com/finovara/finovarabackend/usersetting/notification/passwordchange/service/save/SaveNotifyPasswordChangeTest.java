package com.finovara.finovarabackend.usersetting.notification.passwordchange.service.save;

import com.finovara.finovarabackend.accountactivity.settings.model.SettingActivityStatus;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.usersetting.notification.model.NotificationSettings;
import com.finovara.finovarabackend.usersetting.notification.passwordchange.dto.NotifyPasswordChangeDto;
import com.finovara.finovarabackend.usersetting.notification.passwordchange.service.NotifyPasswordChangeService;
import com.finovara.finovarabackend.accountactivity.settings.service.SettingsActivityService;
import com.finovara.finovarabackend.util.service.user.service.UserManagerService;
import com.finovara.finovarabackend.util.service.user.accountmanagment.passwordpolicy.PasswordChangeEmailService;
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
    private PasswordChangeEmailService passwordChangeEmailService;

    @Mock
    private SettingsActivityService settingsActivityService;

    @InjectMocks
    private NotifyPasswordChangeService notifyPasswordChangeService;

    private NotificationSettings notificationSettings;
    private final String EMAIL = "test@test.com";

    @BeforeEach
    void setup() {
        User user = new User();
        notificationSettings = new NotificationSettings();
        user.setNotificationSettings(notificationSettings);

        when(userManagerService.getUserByEmailOrThrow(EMAIL)).thenReturn(user);
    }

    @Test
    void shouldEnableNotifyOnPasswordChange() {
        NotifyPasswordChangeDto dto = new NotifyPasswordChangeDto(true);

        notifyPasswordChangeService.saveNotifyOnPasswordChange(EMAIL, dto);

        assertTrue(notificationSettings.isNotifyOnPasswordChange());
        verify(settingsActivityService).createSettingActivity(EMAIL, SettingActivityStatus.ENABLED, NOTIFICATION_PASSWORD_CHANGED);
    }

    @Test
    void shouldDisableNotifyOnPasswordChange() {
        notificationSettings.setNotifyOnPasswordChange(true);

        NotifyPasswordChangeDto dto = new NotifyPasswordChangeDto(false);

        notifyPasswordChangeService.saveNotifyOnPasswordChange(EMAIL, dto);

        assertFalse(notificationSettings.isNotifyOnPasswordChange());
        verify(settingsActivityService).createSettingActivity(EMAIL, SettingActivityStatus.DISABLED, NOTIFICATION_PASSWORD_CHANGED);
    }
}