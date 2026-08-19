package com.finovara.notificationservice.notificationemail.service.settings.action.accountdeleted.service;

import com.finovara.contracts.authorization.additionalcode.resolver.AdditionalAuthorizationCodeResolver;
import com.finovara.contracts.activity.event.settings.SettingsActivityEvent;
import com.finovara.contracts.model.activity.SettingActivityStatus;
import com.finovara.contracts.model.activity.SettingType;
import com.finovara.notificationservice.notificationemail.service.settings.action.core.AbstractNotificationEmailService;
import com.finovara.notificationservice.notificationemail.dto.NotificationEmailDto;
import com.finovara.notificationservice.notificationemail.dto.UserEmailDataDto;
import com.finovara.notificationservice.notificationemail.model.ActionEmailNotificationType;
import com.finovara.notificationservice.notificationemail.model.NotificationEmailSettings;
import com.finovara.notificationservice.notificationemail.repository.NotificationEmailSettingsRepository;
import com.finovara.notificationservice.notificationemail.service.settings.NotificationSettingEmailSender;
import com.finovara.notificationservice.notificationemail.service.EmailNotifier;
import com.finovara.notificationservice.feignclient.AuthBackendClient;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

@Service
public class NotifyOnAccountDeletedService extends AbstractNotificationEmailService {
    private final EmailNotifier emailNotifier;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public NotifyOnAccountDeletedService(NotificationEmailSettingsRepository notificationEmailSettingsRepository, NotificationSettingEmailSender notificationSettingEmailSender,
                                         AuthBackendClient authBackendClient, KafkaTemplate<String, Object> kafkaTemplate, EmailNotifier emailNotifier, AdditionalAuthorizationCodeResolver additionalAuthorizationCodeResolver) {
        super(notificationEmailSettingsRepository, notificationSettingEmailSender, authBackendClient, additionalAuthorizationCodeResolver);
        this.emailNotifier = emailNotifier;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    protected boolean isEnabled(NotificationEmailDto dto) {
        return dto.enabled();
    }

    @Override
    protected void applySetting(NotificationEmailSettings settings, boolean value) {
        settings.setNotifyOnAccountDeleted(value);
    }

    @Override
    protected boolean isNotificationEmailSettingsEnabled(NotificationEmailSettings settings) {
        return settings.isNotifyOnAccountDeleted();
    }

    @Override
    protected NotificationEmailDto mapToDto(NotificationEmailSettings settings) {
        return new NotificationEmailDto(settings.isNotifyOnAccountDeleted(), null);
    }

    @Override
    protected void sendEmailToUser(Long userId, UserEmailDataDto userEmailData) {
        emailNotifier.send(ActionEmailNotificationType.ACCOUNT_DELETED, userEmailData.email(),
                Map.of("username", userEmailData.username(), "email", userEmailData.email()));
    }

    @Override
    protected void handleActivity(Long userId, boolean enabled) {
        kafkaTemplate.send("activity.settings", new SettingsActivityEvent(userId, SettingType.NOTIFICATION_ACCOUNT_DELETED, enabled ? SettingActivityStatus.ENABLED : SettingActivityStatus.DISABLED, LocalDateTime.now()));
    }
}
