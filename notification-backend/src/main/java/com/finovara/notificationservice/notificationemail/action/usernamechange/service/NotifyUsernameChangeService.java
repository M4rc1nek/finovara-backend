package com.finovara.notificationservice.notificationemail.action.usernamechange.service;

import com.finovara.contracts.event.activity.settings.SettingsActivityEvent;
import com.finovara.contracts.model.activity.SettingActivityStatus;
import com.finovara.contracts.model.activity.SettingType;
import com.finovara.notificationservice.notificationemail.core.AbstractNotificationEmailService;
import com.finovara.notificationservice.notificationemail.dto.NotificationEmailDto;
import com.finovara.notificationservice.notificationemail.dto.UserEmailDataDto;
import com.finovara.notificationservice.notificationemail.model.EmailNotificationType;
import com.finovara.notificationservice.notificationemail.model.NotificationEmailSettings;
import com.finovara.notificationservice.notificationemail.repository.NotificationEmailSettingsRepository;
import com.finovara.notificationservice.notificationemail.util.NotificationEmailSender;
import com.finovara.notificationservice.notificationemail.util.emailsender.EmailNotifier;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class NotifyUsernameChangeService extends AbstractNotificationEmailService {

    private final EmailNotifier emailNotifier;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public NotifyUsernameChangeService(NotificationEmailSettingsRepository notificationEmailSettingsRepository, NotificationEmailSender notificationEmailSender, EmailNotifier emailNotifier, KafkaTemplate<String, Object> kafkaTemplate) {
        super(notificationEmailSettingsRepository, notificationEmailSender);
        this.emailNotifier = emailNotifier;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    protected boolean isEnabled(NotificationEmailDto dto) {
        return dto.enabled();
    }

    @Override
    protected void applySetting(NotificationEmailSettings settings, boolean value) {
        settings.setNotifyOnUsernameChange(value);
    }

    @Override
    protected boolean isNotificationEmailSettingsEnabled(NotificationEmailSettings settings) {
        return settings.isNotifyOnUsernameChange();
    }

    @Override
    protected NotificationEmailDto mapToDto(NotificationEmailSettings settings) {
        return new NotificationEmailDto(settings.isNotifyOnUsernameChange());
    }

    @Override
    protected void sendEmailToUser(Long userId, UserEmailDataDto userEmailData) {
        emailNotifier.send(EmailNotificationType.USERNAME_CHANGED,userId, userEmailData.username(), userEmailData.email());
    }

    @Override
    protected void handleActivity(Long userId, boolean enabled) {
        kafkaTemplate.send("activity.settings", new SettingsActivityEvent(userId, SettingType.NOTIFICATION_USERNAME_CHANGED, enabled ? SettingActivityStatus.ENABLED : SettingActivityStatus.DISABLED, LocalDateTime.now()));
    }
}
