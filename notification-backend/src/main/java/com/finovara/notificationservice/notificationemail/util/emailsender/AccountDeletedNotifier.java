package com.finovara.notificationservice.notificationemail.util.emailsender;

import com.finovara.notificationservice.notificationemail.util.emailtemplate.EmailTemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AccountDeletedNotifier {
    private static final String TEMPLATE_PATH = "email/account-deleted.html";

    private final EmailTemplateService emailTemplateService;

    @Async
    public void sendEmail(Long userId, String username, String email) {
        emailTemplateService.sendEmail(email, "Finovara - Usunięcie konta", TEMPLATE_PATH, username, email);
    }
}
