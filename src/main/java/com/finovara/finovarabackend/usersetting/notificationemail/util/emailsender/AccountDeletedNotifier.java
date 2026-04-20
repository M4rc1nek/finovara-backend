package com.finovara.finovarabackend.usersetting.notificationemail.util.emailsender;

import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.usersetting.notificationemail.util.emailtemplate.EmailTemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AccountDeletedNotifier {
    private static final String TEMPLATE_PATH = "email/account-deleted.html";

    private final EmailTemplateService emailTemplateService;

    @Async
    public void sendEmail(User user) {
        emailTemplateService.sendEmail(user, "Finovara - Usunięcie konta", TEMPLATE_PATH, user.getUsername(), user.getEmail());
    }
}
