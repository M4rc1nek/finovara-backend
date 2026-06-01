package com.finovara.notificationservice.kafka;

import com.finovara.contracts.event.notification.CreateDefaultNotificationEmailSettingsEvent;
import com.finovara.contracts.event.notification.SendEmailEvent;
import com.finovara.notificationservice.notificationemail.service.NotificationEmailEventService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationEmailConsumer {

    private final NotificationEmailEventService notificationEmailEventService;

    @KafkaListener(topics = "notification.email-settings.create-default", groupId = "notification-service")
    public void createDefaultNotificationEmailSettings(CreateDefaultNotificationEmailSettingsEvent event) {
        notificationEmailEventService.createDefaultNotificationSettings(event.userId());
    }

    @KafkaListener(topics = "notification.email.send", groupId = "notification-service")
    public void sendEmail(SendEmailEvent event) {
        notificationEmailEventService.sendEmail(event);
    }
}
