package com.finovara.notificationservice.notificationemail.service;

import com.finovara.contracts.event.notification.SendEmailEvent;
import com.finovara.notificationservice.notificationemail.model.NotificationEmailSettings;
import com.finovara.notificationservice.notificationemail.repository.NotificationEmailSettingsRepository;
import com.finovara.notificationservice.notificationemail.util.emailtemplate.EmailTemplateService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationEmailEventService {

    private static final String EMAIL_CHANGED_TEMPLATE = "email/email-changed.html";
    private static final String PASSWORD_CHANGED_TEMPLATE = "email/password-changed.html";
    private static final String USERNAME_CHANGED_TEMPLATE = "email/username-changed.html";
    private static final String ACCOUNT_DELETED_TEMPLATE = "email/account-deleted.html";

    private final NotificationEmailSettingsRepository notificationEmailSettingsRepository;
    private final EmailTemplateService emailTemplateService;

    @Transactional
    public void createDefaultNotificationSettings(Long userId) {
        if (notificationEmailSettingsRepository.existsByUserId(userId)) {
            return;
        }

        notificationEmailSettingsRepository.save(NotificationEmailSettings.builder()
                .userId(userId)
                .notifyOnPasswordChange(false)
                .notifyOnUsernameChange(false)
                .notifyOnEmailChange(false)
                .notifyOnAccountDeleted(false)
                .build());
    }

    @Transactional
    public void sendEmail(SendEmailEvent event) {
        Long userId = tryParseUserId(event.to());
        if (userId == null) {
            emailTemplateService.sendEmail(event.to(), event.subject(), event.templateName(), event.username(), event.email());
            return;
        }

        notificationEmailSettingsRepository.findByUserId(userId)
                .filter(settings -> isEnabled(settings, event.templateName()))
                .ifPresent(settings -> {
                    emailTemplateService.sendEmail(event.email(), event.subject(), event.templateName(), event.username(), event.email());
                    if (ACCOUNT_DELETED_TEMPLATE.equals(event.templateName())) {
                        notificationEmailSettingsRepository.deleteByUserId(userId);
                    }
                });
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

    private Long tryParseUserId(String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException exception) {
            log.debug("SendEmailEvent.to does not contain a user id, sending without notification settings check");
            return null;
        }
    }
}
