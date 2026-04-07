package com.finovara.finovarabackend.usersetting.notificationemail.accountdeleted.service.save;

import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.usersetting.notificationemail.accountdeleted.dto.NotifyOnAccountDeletedDto;
import com.finovara.finovarabackend.usersetting.notificationemail.model.NotificationEmailSettings;
import com.finovara.finovarabackend.usersetting.notificationemail.accountdeleted.service.NotifyOnAccountDeletedService;
import com.finovara.finovarabackend.util.user.service.UserManagerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SaveNotifyOnAccountDeletedTest {

    @Mock
    private UserManagerService userManagerService;

    @InjectMocks
    private NotifyOnAccountDeletedService notifyOnAccountDeletedService;

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
    void shouldEnableNotifyOnAccountDeleted() {
        NotifyOnAccountDeletedDto dto = new NotifyOnAccountDeletedDto(true);

        notifyOnAccountDeletedService.saveNotifyOnAccountDeleted(EMAIL, dto);

        assertTrue(notificationEmailSettings.isNotifyOnAccountDeleted());
    }

    @Test
    void shouldDisableNotifyOnAccountDeleted() {
        notificationEmailSettings.setNotifyOnAccountDeleted(true);

        NotifyOnAccountDeletedDto dto = new NotifyOnAccountDeletedDto(false);

        notifyOnAccountDeletedService.saveNotifyOnAccountDeleted(EMAIL, dto);

        assertFalse(notificationEmailSettings.isNotifyOnAccountDeleted());
    }
}