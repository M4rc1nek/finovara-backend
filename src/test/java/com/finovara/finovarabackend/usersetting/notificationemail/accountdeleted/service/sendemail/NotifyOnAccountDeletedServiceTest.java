package com.finovara.finovarabackend.usersetting.notificationemail.accountdeleted.service.sendemail;

import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.usersetting.account.service.passwordpolicy.util.NotificationEmailSender;
import com.finovara.finovarabackend.usersetting.notificationemail.accountdeleted.service.NotifyOnAccountDeletedService;
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
        notifyOnAccountDeletedService.sendEmailOnAccountDeleted(user);
        verify(notificationEmailSender).sendIfEnabled(eq(user), any(), any());
    }

}