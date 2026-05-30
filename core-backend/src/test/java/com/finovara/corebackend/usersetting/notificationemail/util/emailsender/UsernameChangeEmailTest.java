package com.finovara.corebackend.usersetting.notificationemail.util.emailsender;

import com.finovara.corebackend.user.model.User;
import com.finovara.corebackend.usersetting.notificationemail.util.emailtemplate.EmailTemplateService;
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
    private UsernameChangeNotifier usernameChangeNotifier;

    @Test
    void shouldCallSendEmailWithCorrectParameters() {
        User user = new User();
        user.setUsername("john_doe");
        user.setEmail("john@example.com");

        usernameChangeNotifier.sendEmail(user);

        verify(emailTemplateService).sendEmail(
                user,
                "Finovara - Zmiana nazwy użytkownika",
                "email/username-changed.html",
                "john_doe",
                "john@example.com"
        );
    }
}