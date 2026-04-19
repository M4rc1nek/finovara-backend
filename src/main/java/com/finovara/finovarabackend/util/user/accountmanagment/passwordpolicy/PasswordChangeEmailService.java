package com.finovara.finovarabackend.util.user.accountmanagment.passwordpolicy;

import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.util.user.accountmanagment.emailtemplate.EmailTemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PasswordChangeEmailService {
    private static final String TEMPLATE_PATH = "email/password/password-changed.html";

    private final EmailTemplateService emailTemplateService;

    @Async
    public void sendEmail(User user) {
        emailTemplateService.sendEmail(user, "Finovara - Zmiana hasła", TEMPLATE_PATH, user.getUsername(),null);
    }
}
