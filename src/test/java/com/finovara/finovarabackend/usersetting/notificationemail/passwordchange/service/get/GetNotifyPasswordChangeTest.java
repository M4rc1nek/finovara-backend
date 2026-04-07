package com.finovara.finovarabackend.usersetting.notificationemail.passwordchange.service.get;

import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.usersetting.notificationemail.model.NotificationEmailSettings;
import com.finovara.finovarabackend.usersetting.notificationemail.passwordchange.dto.NotifyPasswordChangeDto;
import com.finovara.finovarabackend.usersetting.notificationemail.passwordchange.service.NotifyPasswordChangeService;
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
class GetNotifyPasswordChangeTest {

    @Mock
    private UserManagerService userManagerService;

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
    void shouldReturnEnabledFlag() {
        notificationEmailSettings.setNotifyOnPasswordChange(true);

        NotifyPasswordChangeDto dto = notifyPasswordChangeService.getEmailOnPasswordChange(EMAIL);

        assertTrue(dto.notifyOnPasswordChange());
    }

    @Test
    void shouldReturnDisabledFlag() {
        notificationEmailSettings.setNotifyOnPasswordChange(false);

        NotifyPasswordChangeDto dto = notifyPasswordChangeService.getEmailOnPasswordChange(EMAIL);

        assertFalse(dto.notifyOnPasswordChange());
    }
}