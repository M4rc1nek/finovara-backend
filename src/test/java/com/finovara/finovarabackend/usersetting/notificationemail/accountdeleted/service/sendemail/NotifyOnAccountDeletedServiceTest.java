package com.finovara.finovarabackend.usersetting.notificationemail.accountdeleted.service.sendemail;

import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.usersetting.notificationemail.action.accountdeleted.service.NotifyOnAccountDeletedService;
import com.finovara.finovarabackend.usersetting.notificationemail.util.NotificationEmailSender;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotifyOnAccountDeletedServiceTest {

    @Mock
    private NotificationEmailSender notificationEmailSender;

    @InjectMocks
    private NotifyOnAccountDeletedService notifyOnAccountDeletedService;

    @Test
    void shouldCallSenderToSendEmail() {
        User user = new User();
        notifyOnAccountDeletedService.sendEmail(user);
        verify(notificationEmailSender).sendIfEnabled(eq(user), any(), any());
    }

}