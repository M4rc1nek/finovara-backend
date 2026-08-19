package com.finovara.notificationservice.notificationemail.service.settings.action.passwordchange.service;

import com.finovara.contracts.authorization.additionalcode.resolver.AdditionalAuthorizationCodeResolver;
import com.finovara.contracts.activity.event.settings.SettingsActivityEvent;
import com.finovara.contracts.model.activity.SettingActivityStatus;
import com.finovara.contracts.model.activity.SettingType;
import com.finovara.notificationservice.feignclient.AuthBackendClient;
import com.finovara.notificationservice.notificationemail.dto.NotificationEmailDto;
import com.finovara.notificationservice.notificationemail.model.NotificationEmailSettings;
import com.finovara.notificationservice.notificationemail.repository.NotificationEmailSettingsRepository;
import com.finovara.notificationservice.notificationemail.service.settings.action.core.AbstractActionNotificationEmailService;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class NotifyPasswordChangeServiceAction extends AbstractActionNotificationEmailService {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public NotifyPasswordChangeServiceAction(NotificationEmailSettingsRepository notificationEmailSettingsRepository,
                                             AuthBackendClient authBackendClient,
                                             KafkaTemplate<String, Object> kafkaTemplate,
                                             AdditionalAuthorizationCodeResolver additionalAuthorizationCodeResolver) {
        super(notificationEmailSettingsRepository, authBackendClient, additionalAuthorizationCodeResolver);
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    protected boolean isEnabled(NotificationEmailDto dto) {
        return dto.enabled();
    }

    @Override
    protected void applySetting(NotificationEmailSettings settings, boolean value) {
        settings.setNotifyOnPasswordChange(value);
    }

    @Override
    protected NotificationEmailDto mapToDto(NotificationEmailSettings settings) {
        return new NotificationEmailDto(settings.isNotifyOnPasswordChange(), null);
    }

    @Override
    protected void handleActivity(Long userId, boolean enabled) {
        kafkaTemplate.send("activity.settings", new SettingsActivityEvent(userId, SettingType.NOTIFICATION_PASSWORD_CHANGED, enabled ? SettingActivityStatus.ENABLED : SettingActivityStatus.DISABLED, LocalDateTime.now()));
    }
}