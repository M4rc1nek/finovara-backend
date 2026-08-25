package com.finovara.notificationservice.notificationemail.service.settings;

import com.finovara.contracts.notification.email.ActionEmailEventType;
import com.finovara.contracts.notification.event.SendEmailEvent;
import com.finovara.contracts.user.event.UserCreatedEvent;
import com.finovara.contracts.user.event.account.delete.UserAccountDeletedEvent;
import com.finovara.notificationservice.notificationemail.model.ActionEmailNotificationType;
import com.finovara.notificationservice.notificationemail.model.NotificationEmailSettings;
import com.finovara.notificationservice.notificationemail.repository.NotificationEmailSettingsRepository;
import com.finovara.notificationservice.notificationemail.service.EmailNotifier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationSettingEmailConsumer {
    private final NotificationEmailSettingsRepository notificationEmailSettingsRepository;
    private final NotificationEmailSettingsService notificationEmailSettingsService;
    private final EmailNotifier emailNotifier;

    @KafkaListener(topics = "user.created")
    public void handleUserCreated(UserCreatedEvent event) {
        notificationEmailSettingsService.createSettingsIfNotExist(event.userId());
    }

    @KafkaListener(topics = "notification.email.send")
    public void sendEmail(SendEmailEvent event) {
        notificationEmailSettingsRepository.findByUserId(event.userId())
                .filter(settings -> isEnabled(settings, event.eventType()))
                .ifPresent(settings -> processEmail(event));
    }

    @KafkaListener(topics = "user-account.deleted", groupId = "notification-email-service")
    public void deleteSettings(UserAccountDeletedEvent event) {
        notificationEmailSettingsRepository.findByUserId(event.userId())
                .ifPresent(notificationEmailSettingsService::deleteSettings);
    }

    private boolean isEnabled(NotificationEmailSettings settings, ActionEmailEventType eventType) {
        return switch (eventType) {
            case EMAIL_CHANGED -> settings.isNotifyOnEmailChange();
            case PASSWORD_CHANGED -> settings.isNotifyOnPasswordChange();
            case USERNAME_CHANGED -> settings.isNotifyOnUsernameChange();
            case ACCOUNT_DELETED -> settings.isNotifyOnAccountDeleted();
            case WALLET_LOW_BALANCE -> settings.isNotifyOnWalletLowBalance();
            case SHARED_ACCOUNT_LARGE_EXPENSE_DETECTED, SHARED_ACCOUNT_PIGGY_BANK_GOAL_ACHIEVED -> true;
        };
    }

    private void processEmail(SendEmailEvent event) {
        ActionEmailNotificationType template = ActionEmailNotificationType.valueOf(event.eventType().name());

        Map<String, String> placeholders = new HashMap<>(event.placeholders());
        placeholders.putIfAbsent("username", event.username());
        placeholders.putIfAbsent("email", event.email());

        emailNotifier.send(template, event.email(), placeholders);
    }
}