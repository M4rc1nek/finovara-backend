package com.finovara.notificationservice.notificationemail.util.emailsender;

import com.finovara.notificationservice.notificationemail.util.emailtemplate.EmailTemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailChangeNotifier {
    private static final String TEMPLATE_PATH = "email/email-changed.html";

    private final EmailTemplateService emailTemplateService;

    @Async
    public void sendEmail(Long userId, String username, String email) {
        emailTemplateService.sendEmail(email, "Finovara - Zmiana adresu e-mail", TEMPLATE_PATH, username, email);
    }
}
