package com.finovara.finovarabackend.usersetting.notificationemail.util.emailsender;

import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.usersetting.notificationemail.util.emailtemplate.EmailTemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailChangeNotifier {
    private static final String TEMPLATE_PATH = "email/emailchange/email-changed.html";

    private final EmailTemplateService emailTemplateService;

    @Async
    public void sendEmail(User user) {
        emailTemplateService.sendEmail(user, "Finovara - Zmiana adresu e-mail", TEMPLATE_PATH, user.getUsername(),user.getEmail());
    }
}
