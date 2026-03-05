package com.finovara.finovarabackend.util.service.user.accountmanagment.usernamepolicy;

import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.util.service.user.accountmanagment.emailtemplate.EmailTemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UsernameChangeEmailService {
    private static final String TEMPLATE_PATH = "email/username-changed.html";

    private final EmailTemplateService emailTemplateService;

    @Async
    public void sendEmail(User user) {
        emailTemplateService.sendEmail(user, "Finovara - Zmiana nazwy użytkownika", TEMPLATE_PATH, user.getUsername(), user.getEmail());
    }
}
