package com.finovara.finovarabackend.util.service.user.accountmanagment.accountpolicy.accountdeleted;

import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.util.service.user.accountmanagment.emailtemplate.EmailTemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AccountDeletedEmailService {
    private static final String TEMPLATE_PATH = "email/account-deleted.html";

    private final EmailTemplateService emailTemplateService;

    @Async
    public void sendEmail(User user) {
        emailTemplateService.sendEmail(user, "Finovara - Usunięcie konta", TEMPLATE_PATH, user.getUsername(), user.getEmail());
    }
}
