package com.finovara.finovarabackend.usersetting.notificationemail.util.emailsender;

import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.usersetting.notificationemail.util.emailtemplate.EmailTemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PasswordChangeNotifier {
    private static final String TEMPLATE_PATH = "email/password/password-changed.html";

    private final EmailTemplateService emailTemplateService;

    @Async
    public void sendEmail(User user) {
        emailTemplateService.sendEmail(user, "Finovara - Zmiana hasła", TEMPLATE_PATH, user.getUsername(),null);
    }
}
