package com.finovara.finovarabackend.usersetting.notificationemail.passwordchange.service.sendemail;

import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.usersetting.account.service.passwordpolicy.util.NotificationEmailSender;
import com.finovara.finovarabackend.usersetting.notificationemail.passwordchange.service.NotifyPasswordChangeService;
import com.finovara.finovarabackend.util.user.accountmanagment.passwordpolicy.PasswordChangeEmailService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotifyPasswordChangeServiceTest {
    @Mock
    private NotificationEmailSender notificationEmailSender;

    @Mock
    private PasswordChangeEmailService passwordChangeEmailService;

    @InjectMocks
    private NotifyPasswordChangeService notifyPasswordChangeService;

    @Test
    void shouldCallSenderToSendEmail() {
        User user = new User();
        notifyPasswordChangeService.sendEmailOnPasswordChange(user);
        verify(notificationEmailSender).sendIfEnabled(eq(user), any(), any());
    }

}