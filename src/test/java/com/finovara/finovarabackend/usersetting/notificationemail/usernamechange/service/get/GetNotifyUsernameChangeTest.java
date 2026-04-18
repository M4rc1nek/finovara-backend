package com.finovara.finovarabackend.usersetting.notificationemail.usernamechange.service.get;

import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.usersetting.notificationemail.model.NotificationEmailSettings;
import com.finovara.finovarabackend.usersetting.notificationemail.dto.NotificationEmailDto;
import com.finovara.finovarabackend.usersetting.notificationemail.usernamechange.service.NotifyUsernameChangeService;
import com.finovara.finovarabackend.util.user.service.UserManagerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetNotifyUsernameChangeTest {

    @Mock
    private UserManagerService userManagerService;

    @InjectMocks
    private NotifyUsernameChangeService notifyUsernameChangeService;

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
    @CsvSource({"true, ENABLED",
            "false, DISABLED"
    })
    void shouldReturnNotificationFlagBasedOnSettings(boolean enabled) {
        notificationEmailSettings.setNotifyOnUsernameChange(enabled);

        NotificationEmailDto dto = notifyUsernameChangeService.getEmailNotification(USER_ID);

        assertEquals(dto.enabled(), enabled);
    }
}