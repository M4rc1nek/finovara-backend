package com.finovara.finovarabackend.usersetting.notificationemail.action.usernamechange.service;

import com.finovara.finovarabackend.accountactivity.settings.model.SettingActivityStatus;
import com.finovara.finovarabackend.accountactivity.settings.service.SettingsActivityService;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.usersetting.notificationemail.dto.NotificationEmailDto;
import com.finovara.finovarabackend.usersetting.notificationemail.model.NotificationEmailSettings;
import com.finovara.finovarabackend.usersetting.notificationemail.util.NotificationEmailSender;
import com.finovara.finovarabackend.util.user.service.UserManagerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.finovara.finovarabackend.accountactivity.settings.model.SettingType.NOTIFICATION_USERNAME_CHANGED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotifyUsernameChangeServiceTest {

    @Mock
    private NotificationEmailSender notificationEmailSender;

    @Mock
    private UserManagerService userManagerService;

    @Mock
    private SettingsActivityService settingsActivityService;

    @InjectMocks
    private NotifyUsernameChangeService notifyUsernameChangeService;

    private User user;
    private Long userId;
    private NotificationEmailSettings notificationEmailSettings;

    @BeforeEach
    void setup() {
        userId = 1L;

        user = new User();
        user.setId(userId);

        notificationEmailSettings = new NotificationEmailSettings();
        user.setNotificationEmailSettings(notificationEmailSettings);
    }

    @Nested
    class SaveUsernameChangeTest {

        @ParameterizedTest
        @CsvSource({"true, ENABLED", "false, DISABLED"})
        void shouldSaveAndCreateActivity(boolean enabled, SettingActivityStatus expectedStatus) {
            when(userManagerService.getUserByIdOrThrow(userId)).thenReturn(user);

            NotificationEmailDto dto = new NotificationEmailDto(enabled);

            notifyUsernameChangeService.saveEmailNotification(userId, dto);

            assertEquals(enabled, notificationEmailSettings.isNotifyOnUsernameChange());

            verify(settingsActivityService).createSettingActivity(userId, expectedStatus, NOTIFICATION_USERNAME_CHANGED);
        }
    }

    @Nested
    class GetUsernameChangeTest {

        @Test
        void shouldReturnEnabled() {
            when(userManagerService.getUserByIdOrThrow(userId)).thenReturn(user);

            notificationEmailSettings.setNotifyOnUsernameChange(true);

            NotificationEmailDto dto = notifyUsernameChangeService.getEmailNotification(userId);

            assertEquals(true, dto.enabled());
        }

        @Test
        void shouldReturnDisabled() {
            when(userManagerService.getUserByIdOrThrow(userId)).thenReturn(user);

            notificationEmailSettings.setNotifyOnUsernameChange(false);

            NotificationEmailDto dto = notifyUsernameChangeService.getEmailNotification(userId);

            assertEquals(false, dto.enabled());
        }
    }

    @Nested
    class SendEmailTest {

        @Test
        void shouldCallSenderToSendEmail() {
            notifyUsernameChangeService.sendEmail(user);

            verify(notificationEmailSender).sendIfEnabled(eq(user), any(), any());
        }
    }
}