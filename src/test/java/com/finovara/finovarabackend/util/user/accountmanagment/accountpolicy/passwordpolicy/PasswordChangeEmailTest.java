package com.finovara.finovarabackend.util.user.accountmanagment.accountpolicy.passwordpolicy;

import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.usersetting.notificationemail.util.emailtemplate.EmailTemplateService;
import com.finovara.finovarabackend.usersetting.notificationemail.util.emailsender.PasswordChangeNotifier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PasswordChangeEmailTest {

    @Mock
    private EmailTemplateService emailTemplateService;

    @InjectMocks
    private PasswordChangeNotifier passwordChangeNotifier;

    @Test
    void shouldCallSendEmailWithCorrectParameters() {
        User user = new User();
        user.setUsername("john_doe");
        user.setEmail("john@example.com");

        passwordChangeNotifier.sendEmail(user);

        verify(emailTemplateService).sendEmail(
                user,
                "Finovara - Zmiana hasła",
                "email/password/password-changed.html",
                "john_doe",
                null
        );
    }
}