package com.finovara.corebackend.notification.email;

import com.finovara.contracts.event.notification.CreateDefaultNotificationEmailSettingsEvent;
import com.finovara.contracts.event.notification.SendEmailEvent;
import com.finovara.corebackend.user.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationEmailEventPublisher {

    private static final String CREATE_DEFAULT_SETTINGS_TOPIC = "notification.email-settings.create-default";
    private static final String SEND_EMAIL_TOPIC = "notification.email.send";

    private static final String EMAIL_CHANGED_TEMPLATE = "email/email-changed.html";
    private static final String PASSWORD_CHANGED_TEMPLATE = "email/password-changed.html";
    private static final String USERNAME_CHANGED_TEMPLATE = "email/username-changed.html";
    private static final String ACCOUNT_DELETED_TEMPLATE = "email/account-deleted.html";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void createDefaultSettings(Long userId) {
        kafkaTemplate.send(CREATE_DEFAULT_SETTINGS_TOPIC, new CreateDefaultNotificationEmailSettingsEvent(userId));
    }

    public void sendEmailChanged(User user) {
        send(user, "Finovara - Zmiana adresu e-mail", EMAIL_CHANGED_TEMPLATE);
    }

    public void sendPasswordChanged(User user) {
        send(user, "Finovara - Zmiana hasla", PASSWORD_CHANGED_TEMPLATE);
    }

    public void sendUsernameChanged(User user) {
        send(user, "Finovara - Zmiana nazwy uzytkownika", USERNAME_CHANGED_TEMPLATE);
    }

    public void sendAccountDeleted(User user) {
        send(user, "Finovara - Usuniecie konta", ACCOUNT_DELETED_TEMPLATE);
    }

    private void send(User user, String subject, String templateName) {
        kafkaTemplate.send(SEND_EMAIL_TOPIC, new SendEmailEvent(
                user.getId().toString(),
                user.getUsername(),
                user.getEmail(),
                subject,
                templateName
        ));
    }
}
