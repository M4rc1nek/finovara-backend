package com.finovara.finovarabackend.util.manager.service.user.accountmanagment.accountpolicy.passwordpolicy;

import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.util.user.accountmanagment.emailtemplate.EmailTemplateService;
import com.finovara.finovarabackend.util.user.accountmanagment.passwordpolicy.PasswordChangeEmailService;
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
    private PasswordChangeEmailService passwordChangeEmailService;

    @Test
    void shouldCallSendEmailWithCorrectParameters() {
        User user = new User();
        user.setUsername("john_doe");
        user.setEmail("john@example.com");

        passwordChangeEmailService.sendEmail(user);

        verify(emailTemplateService).sendEmail(
                user,
                "Finovara - Zmiana hasła",
                "email/password-changed.html",
                "john_doe",
                null
        );
    }
}