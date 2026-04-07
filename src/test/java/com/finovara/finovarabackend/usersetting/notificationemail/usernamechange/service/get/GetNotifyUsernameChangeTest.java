package com.finovara.finovarabackend.usersetting.notificationemail.usernamechange.service.get;

import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.usersetting.notificationemail.model.NotificationSettings;
import com.finovara.finovarabackend.usersetting.notificationemail.usernamechange.dto.NotifyUsernameChangeDto;
import com.finovara.finovarabackend.usersetting.notificationemail.usernamechange.service.NotifyUsernameChangeService;
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

    @InjectMocks
    private NotifyUsernameChangeService notifyUsernameChangeService;

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
    void shouldReturnEnabledFlag() {
        notificationSettings.setNotifyOnUsernameChange(true);

        NotifyUsernameChangeDto dto = notifyUsernameChangeService.getEmailOnUsernameChange(EMAIL);

        assertTrue(dto.notifyOnUsernameChange());
    }

    @Test
    void shouldReturnDisabledFlag() {
        notificationSettings.setNotifyOnUsernameChange(false);

        NotifyUsernameChangeDto dto = notifyUsernameChangeService.getEmailOnUsernameChange(EMAIL);

        assertFalse(dto.notifyOnUsernameChange());
    }
}