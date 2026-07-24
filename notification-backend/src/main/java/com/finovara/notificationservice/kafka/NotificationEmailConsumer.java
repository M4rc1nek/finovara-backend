package com.finovara.notificationservice.kafka;

import com.finovara.contracts.event.notification.SendEmailEvent;
import com.finovara.contracts.event.user.delete.account.UserAccountDeletedEvent;
import com.finovara.contracts.event.user.UserCreatedEvent;
import com.finovara.notificationservice.notificationemail.model.NotificationEmailSettings;
import com.finovara.notificationservice.notificationemail.repository.NotificationEmailSettingsRepository;
import com.finovara.notificationservice.notificationemail.service.NotificationEmailSettingsService;
import com.finovara.notificationservice.notificationemail.util.emailtemplate.EmailTemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEmailConsumer {

    private static final String EMAIL_CHANGED_TEMPLATE = "email/email-changed.html";
    private static final String PASSWORD_CHANGED_TEMPLATE = "email/password-changed.html";
    private static final String USERNAME_CHANGED_TEMPLATE = "email/username-changed.html";
    private static final String ACCOUNT_DELETED_TEMPLATE = "email/account-deleted.html";

    private final NotificationEmailSettingsRepository notificationEmailSettingsRepository;
    private final NotificationEmailSettingsService notificationEmailSettingsService;
    private final EmailTemplateService emailTemplateService;

    @KafkaListener(topics = "user.created")
    public void handleUserCreated(UserCreatedEvent event) {
        notificationEmailSettingsService.createSettingsIfNotExist(event.userId());
    }

    @KafkaListener(topics = "notification.email.send")
    public void sendEmail(SendEmailEvent event) {
        notificationEmailSettingsRepository.findByUserId(event.userId())
                .filter(settings -> isEnabled(settings, event.templateName()))
                .ifPresent(settings -> processEmail(event));
    }

    @KafkaListener(topics = "user-account.deleted", groupId = "notification-email-service")
    public void deleteSettings(UserAccountDeletedEvent event) {
        notificationEmailSettingsRepository.findByUserId(event.userId())
                .ifPresent(notificationEmailSettingsService::deleteSettings);
    }

    private boolean isEnabled(NotificationEmailSettings settings, String templateName) {
        return switch (templateName) {
            case EMAIL_CHANGED_TEMPLATE -> settings.isNotifyOnEmailChange();
            case PASSWORD_CHANGED_TEMPLATE -> settings.isNotifyOnPasswordChange();
            case USERNAME_CHANGED_TEMPLATE -> settings.isNotifyOnUsernameChange();
            case ACCOUNT_DELETED_TEMPLATE -> settings.isNotifyOnAccountDeleted();
            default -> true;
        };
    }

    private void processEmail(SendEmailEvent event) {
        emailTemplateService.sendEmail(
                event.email(),
                event.subject(),
                event.templateName(),
                event.username(),
                event.email());
    }
}