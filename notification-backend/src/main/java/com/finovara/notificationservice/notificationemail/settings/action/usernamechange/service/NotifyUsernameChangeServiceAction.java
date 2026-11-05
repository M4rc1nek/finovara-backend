package com.finovara.notificationservice.notificationemail.settings.action.usernamechange.service;

import com.finovara.contracts.user.authorization.additionalcode.resolver.AdditionalAuthorizationCodeResolver;
import com.finovara.contracts.mainaccount.activity.model.SettingType;
import com.finovara.contracts.outbox.OutboxService;
import com.finovara.notificationservice.feignclient.AuthBackendClient;
import com.finovara.notificationservice.notificationemail.dto.NotificationEmailDto;
import com.finovara.notificationservice.notificationemail.model.NotificationEmailSettings;
import com.finovara.notificationservice.notificationemail.settings.repository.NotificationEmailSettingsRepository;
import com.finovara.notificationservice.notificationemail.settings.action.core.AbstractActionNotificationEmailService;
import org.springframework.stereotype.Service;

@Service
public class NotifyUsernameChangeServiceAction extends AbstractActionNotificationEmailService<NotificationEmailDto, NotificationEmailDto> {

    public NotifyUsernameChangeServiceAction(NotificationEmailSettingsRepository notificationEmailSettingsRepository,
                                             AuthBackendClient authBackendClient,
                                             OutboxService outboxService,
                                             AdditionalAuthorizationCodeResolver additionalAuthorizationCodeResolver) {
        super(notificationEmailSettingsRepository, authBackendClient, additionalAuthorizationCodeResolver, outboxService);
    }

    @Override
    protected void applySetting(NotificationEmailSettings settings, NotificationEmailDto dto) {
        settings.setNotifyOnUsernameChange(Boolean.TRUE.equals(dto.enabled()));
    }

    @Override
    protected NotificationEmailDto mapToDto(NotificationEmailSettings settings) {
        return new NotificationEmailDto(settings.isNotifyOnUsernameChange(), null);
    }

    @Override
    protected void handleActivity(Long userId, boolean enabled) {
        publishActivity(userId, SettingType.NOTIFICATION_USERNAME_CHANGED, enabled);
    }
}
