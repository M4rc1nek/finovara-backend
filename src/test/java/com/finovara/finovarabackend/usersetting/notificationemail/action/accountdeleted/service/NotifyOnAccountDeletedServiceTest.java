package com.finovara.finovarabackend.usersetting.notificationemail.action.accountdeleted.service;

import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.usersetting.notificationemail.dto.NotificationEmailDto;
import com.finovara.finovarabackend.usersetting.notificationemail.model.NotificationEmailSettings;
import com.finovara.finovarabackend.usersetting.notificationemail.util.NotificationEmailSender;
import com.finovara.finovarabackend.util.user.service.UserManagerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotifyOnAccountDeletedServiceTest {

    @Mock
    private NotificationEmailSender notificationEmailSender;

    @Mock
    private UserManagerService userManagerService;

    @InjectMocks
    private NotifyOnAccountDeletedService notifyOnAccountDeletedService;

    private Long userId;
    private User user;
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
    class GetNotifyOnAccountDeletedTest {
        @Test
        void shouldReturnEnabled() {
            when(userManagerService.getUserByIdOrThrow(userId)).thenReturn(user);
            notificationEmailSettings.setNotifyOnAccountDeleted(true);

            NotificationEmailDto dto = notifyOnAccountDeletedService.getEmailNotification(userId);

            assertEquals(true, dto.enabled());
        }

        @Test
        void shouldReturnDisabled() {
            when(userManagerService.getUserByIdOrThrow(userId)).thenReturn(user);

            notificationEmailSettings.setNotifyOnAccountDeleted(false);

            NotificationEmailDto dto = notifyOnAccountDeletedService.getEmailNotification(userId);

            assertEquals(false, dto.enabled());
        }
    }

    @Nested
    class SaveNotifyOnAccountDeletedTest {
        @Test
        void shouldSaveTrue() {
            when(userManagerService.getUserByIdOrThrow(userId)).thenReturn(user);

            NotificationEmailDto dto = new NotificationEmailDto(true);

            notifyOnAccountDeletedService.saveEmailNotification(userId, dto);

            assertEquals(true, user.getNotificationEmailSettings().isNotifyOnAccountDeleted());
        }

        @Test
        void shouldSaveFalse() {
            when(userManagerService.getUserByIdOrThrow(userId)).thenReturn(user);

            NotificationEmailDto dto = new NotificationEmailDto(false);

            notifyOnAccountDeletedService.saveEmailNotification(userId, dto);

            assertEquals(false, user.getNotificationEmailSettings().isNotifyOnAccountDeleted());
        }
    }

    @Nested
    class SendEmailTest {
        @Test
        void shouldCallSenderToSendEmail() {
            notifyOnAccountDeletedService.sendEmail(user);

            verify(notificationEmailSender).sendIfEnabled(eq(user), any(), any());
        }
    }
}