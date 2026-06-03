package com.finovara.notificationservice.notificationemail.util.emailsender;

import com.finovara.notificationservice.notificationemail.model.EmailNotificationType;
import com.finovara.notificationservice.notificationemail.util.emailtemplate.EmailTemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailNotifier {

    private static final String ACCOUNT_DELETED_TEMPLATE = "email/account-deleted.html";
    private static final String PASSWORD_CHANGED_TEMPLATE = "email/password-changed.html";
    private static final String USERNAME_CHANGED_TEMPLATE = "email/username-changed.html";
    private static final String EMAIL_CHANGED_TEMPLATE = "email/email-changed.html";

    private final EmailTemplateService emailTemplateService;

    @Async
    public void send(EmailNotificationType type, Long userId, String username, String email) {
        switch (type) {
            case ACCOUNT_DELETED ->
                    emailTemplateService.sendEmail(email, "Finovara - Usunięcie konta", ACCOUNT_DELETED_TEMPLATE, username, email);

            case EMAIL_CHANGED ->
                    emailTemplateService.sendEmail(email, "Finovara - Zmiana adresu e-mail", EMAIL_CHANGED_TEMPLATE, username, email);

            case USERNAME_CHANGED ->
                    emailTemplateService.sendEmail(email, "Finovara - Zmiana nazwy użytkownika", USERNAME_CHANGED_TEMPLATE, username, null);

            case PASSWORD_CHANGED ->
                    emailTemplateService.sendEmail(email, "Finovara - Zmiana hasła", PASSWORD_CHANGED_TEMPLATE, username, null);
        }
    }
}