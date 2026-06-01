package com.finovara.notificationservice.notificationemail.util.emailsender;

import com.finovara.notificationservice.notificationemail.util.emailtemplate.EmailTemplateService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AccountDeletedEmailTest {

    @Mock
    private EmailTemplateService emailTemplateService;

    @InjectMocks
    private AccountDeletedNotifier accountDeletedNotifier;

    @Test
    void shouldCallSendEmailWithCorrectParameters() {
        accountDeletedNotifier.sendEmail(1L, "john_doe", "john@example.com");

        verify(emailTemplateService).sendEmail(
                eq("john@example.com"),
                anyString(),
                eq("email/account-deleted.html"),
                eq("john_doe"),
                eq("john@example.com")
        );
    }
}
