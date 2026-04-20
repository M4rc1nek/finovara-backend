package com.finovara.finovarabackend.usersetting.notificationemail.passwordchange.service.save;

import com.finovara.finovarabackend.accountactivity.settings.model.SettingActivityStatus;
import com.finovara.finovarabackend.accountactivity.settings.service.SettingsActivityService;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.usersetting.notificationemail.dto.NotificationEmailDto;
import com.finovara.finovarabackend.usersetting.notificationemail.model.NotificationEmailSettings;
import com.finovara.finovarabackend.usersetting.notificationemail.action.passwordchange.service.NotifyPasswordChangeService;
import com.finovara.finovarabackend.util.user.service.UserManagerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.finovara.finovarabackend.accountactivity.settings.model.SettingType.NOTIFICATION_PASSWORD_CHANGED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SaveNotifyPasswordChangeTest {

    @Mock
    private UserManagerService userManagerService;

    @Mock
    private SettingsActivityService settingsActivityService;

    @InjectMocks
    private NotifyPasswordChangeService notifyPasswordChangeService;

    private NotificationEmailSettings notificationEmailSettings;
    private final Long USER_ID = 1L;

    @BeforeEach
    void setup() {
        User user = new User();
        notificationEmailSettings = new NotificationEmailSettings();
        user.setNotificationEmailSettings(notificationEmailSettings);

        when(userManagerService.getUserByIdOrThrow(USER_ID)).thenReturn(user);
    }

    @ParameterizedTest
    @CsvSource({
            "true, ENABLED",
            "false, DISABLED"
    })
    void shouldSaveUsernameChangeNotificationAndCreateActivity(boolean enabled, SettingActivityStatus expectedStatus) {
        notificationEmailSettings.setNotifyOnPasswordChange(enabled);
        NotificationEmailDto dto = new NotificationEmailDto(enabled);

        notifyPasswordChangeService.saveEmailNotification(USER_ID, dto);
        assertEquals(enabled, notificationEmailSettings.isNotifyOnPasswordChange());

        verify(settingsActivityService).createSettingActivity(
                USER_ID,
                expectedStatus,
                NOTIFICATION_PASSWORD_CHANGED
        );
    }
}