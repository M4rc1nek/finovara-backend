package com.finovara.finovarabackend.usersetting.notification.accountdeleted.service.save;

import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.usersetting.notification.accountdeleted.dto.NotifyOnAccountDeletedDto;
import com.finovara.finovarabackend.usersetting.notification.model.NotificationSettings;
import com.finovara.finovarabackend.usersetting.notification.accountdeleted.service.NotifyOnAccountDeletedService;
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
    void shouldEnableNotifyOnAccountDeleted() {
        NotifyOnAccountDeletedDto dto = new NotifyOnAccountDeletedDto(true);

        notifyOnAccountDeletedService.saveNotifyOnAccountDeleted(EMAIL, dto);

        assertTrue(notificationSettings.isNotifyOnAccountDeleted());
    }

    @Test
    void shouldDisableNotifyOnAccountDeleted() {
        notificationSettings.setNotifyOnAccountDeleted(true);

        NotifyOnAccountDeletedDto dto = new NotifyOnAccountDeletedDto(false);

        notifyOnAccountDeletedService.saveNotifyOnAccountDeleted(EMAIL, dto);

        assertFalse(notificationSettings.isNotifyOnAccountDeleted());
    }
}