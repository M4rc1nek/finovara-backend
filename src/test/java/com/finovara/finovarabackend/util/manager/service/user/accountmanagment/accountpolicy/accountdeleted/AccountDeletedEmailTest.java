package com.finovara.finovarabackend.util.manager.service.user.accountmanagment.accountpolicy.accountdeleted;

import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.util.user.accountmanagment.emailtemplate.EmailTemplateService;
import com.finovara.finovarabackend.util.user.accountmanagment.accountpolicy.accountdeleted.AccountDeletedEmailService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AccountDeletedEmailTest {

    @Mock
    private EmailTemplateService emailTemplateService;

    @InjectMocks
    private AccountDeletedEmailService accountDeletedEmailService;

    @Test
    void shouldCallSendEmailWithCorrectParameters() {
        User user = new User();
        user.setUsername("john_doe");
        user.setEmail("john@example.com");

        accountDeletedEmailService.sendEmail(user);

        verify(emailTemplateService).sendEmail(
                user,
                "Finovara - Usunięcie konta",
                "email/account-deleted.html",
                "john_doe",
                "john@example.com"
        );
    }
}