package com.finovara.finovarabackend.usersetting.notificationemail.usernamechange.service.get;

import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.usersetting.notificationemail.model.NotificationEmailSettings;
import com.finovara.finovarabackend.usersetting.notificationemail.usernamechange.dto.NotifyUsernameChangeDto;
import com.finovara.finovarabackend.usersetting.notificationemail.usernamechange.service.NotifyUsernameChangeService;
import com.finovara.finovarabackend.usersetting.notificationemail.util.NotificationEmailSender;
import com.finovara.finovarabackend.util.user.service.UserManagerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetNotifyUsernameChangeTest {

    @Mock
    private UserManagerService userManagerService;

    @Mock
    private  NotificationEmailSender notificationEmailSender;

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
    void shouldReturnEnabledFlag() {
        notificationEmailSettings.setNotifyOnUsernameChange(true);

        NotifyUsernameChangeDto dto = notifyUsernameChangeService.getEmailNotification(EMAIL);

        assertTrue(dto.notifyOnUsernameChange());
    }

    @Test
    void shouldReturnDisabledFlag() {
        notificationEmailSettings.setNotifyOnUsernameChange(false);

        NotifyUsernameChangeDto dto = notifyUsernameChangeService.getEmailNotification(EMAIL);

        assertFalse(dto.notifyOnUsernameChange());
    }
}