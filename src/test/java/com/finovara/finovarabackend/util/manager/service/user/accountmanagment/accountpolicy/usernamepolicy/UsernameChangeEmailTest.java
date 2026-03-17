package com.finovara.finovarabackend.util.manager.service.user.accountmanagment.accountpolicy.usernamepolicy;

import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.util.service.user.accountmanagment.emailtemplate.EmailTemplateService;
import com.finovara.finovarabackend.util.service.user.accountmanagment.usernamepolicy.UsernameChangeEmailService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UsernameChangeEmailTest {

    @Mock
    private EmailTemplateService emailTemplateService;

    @InjectMocks
    private UsernameChangeEmailService usernameChangeEmailService;

    @Test
    void shouldCallSendEmailWithCorrectParameters() {
        User user = new User();
        user.setUsername("john_doe");
        user.setEmail("john@example.com");

        usernameChangeEmailService.sendEmail(user);

        verify(emailTemplateService).sendEmail(
                user,
                "Finovara - Zmiana nazwy użytkownika",
                "email/username-changed.html",
                "john_doe",
                "john@example.com"
        );
    }
}