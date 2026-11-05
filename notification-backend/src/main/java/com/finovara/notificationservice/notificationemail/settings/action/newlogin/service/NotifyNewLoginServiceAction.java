package com.finovara.notificationservice.notificationemail.settings.action.newlogin.service;

import com.finovara.contracts.mainaccount.activity.model.SettingType;
import com.finovara.contracts.outbox.OutboxService;
import com.finovara.contracts.user.authorization.additionalcode.resolver.AdditionalAuthorizationCodeResolver;
import com.finovara.notificationservice.feignclient.AuthBackendClient;
import com.finovara.notificationservice.notificationemail.dto.NotificationEmailDto;
import com.finovara.notificationservice.notificationemail.model.NotificationEmailSettings;
import com.finovara.notificationservice.notificationemail.settings.repository.NotificationEmailSettingsRepository;
import com.finovara.notificationservice.notificationemail.settings.action.core.AbstractActionNotificationEmailService;
import org.springframework.stereotype.Service;

@Service
public class NotifyNewLoginServiceAction extends AbstractActionNotificationEmailService<NotificationEmailDto, NotificationEmailDto> {

    public NotifyNewLoginServiceAction(NotificationEmailSettingsRepository notificationEmailSettingsRepository,
                                       AuthBackendClient authBackendClient,
                                       OutboxService outboxService,
                                       AdditionalAuthorizationCodeResolver additionalAuthorizationCodeResolver) {
        super(notificationEmailSettingsRepository, authBackendClient, additionalAuthorizationCodeResolver, outboxService);
    }

    @Override
    protected void applySetting(NotificationEmailSettings settings, NotificationEmailDto dto) {
        settings.setNotifyOnNewLogin(Boolean.TRUE.equals(dto.enabled()));
    }

    @Override
    protected NotificationEmailDto mapToDto(NotificationEmailSettings settings) {
        return new NotificationEmailDto(settings.isNotifyOnNewLogin(), null);
    }

    @Override
    protected void handleActivity(Long userId, boolean enabled) {
        publishActivity(userId, SettingType.NOTIFICATION_NEW_LOGIN, enabled);
    }
}
