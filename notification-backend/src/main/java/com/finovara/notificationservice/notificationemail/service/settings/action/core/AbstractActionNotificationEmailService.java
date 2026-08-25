package com.finovara.notificationservice.notificationemail.service.settings.action.core;

import com.finovara.contracts.authorization.additionalcode.resolver.AdditionalAuthorizationCodeResolver;
import com.finovara.contracts.exception.notfound.RequestedEntityNotFoundException;
import com.finovara.notificationservice.notificationemail.model.EmailNotificationSettingRequest;
import com.finovara.notificationservice.notificationemail.model.NotificationEmailSettings;
import com.finovara.notificationservice.notificationemail.repository.NotificationEmailSettingsRepository;
import com.finovara.notificationservice.feignclient.AuthBackendClient;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
public abstract class AbstractActionNotificationEmailService<D extends EmailNotificationSettingRequest, R> {

    protected final NotificationEmailSettingsRepository notificationEmailSettingsRepository;
    protected final AuthBackendClient authBackendClient;
    protected final AdditionalAuthorizationCodeResolver additionalAuthorizationCodeResolver;

    @Transactional
    public void saveEmailNotification(Long userId, D dto) {
        authBackendClient.confirmAuthorizationCode(userId, additionalAuthorizationCodeResolver.resolve(dto.authorizationCode()));

        NotificationEmailSettings settings = notificationEmailSettingsRepository.findByUserId(userId)
                .orElseThrow(() -> new RequestedEntityNotFoundException("Notification email settings not found for userId: " + userId));

        boolean enabled = Boolean.TRUE.equals(dto.enabled());
        applySetting(settings, dto);
        notificationEmailSettingsRepository.save(settings);

        handleActivity(userId, enabled);
    }

    public R getEmailNotification(Long userId) {
        NotificationEmailSettings settings = notificationEmailSettingsRepository.findByUserId(userId)
                .orElseThrow(() -> new RequestedEntityNotFoundException("Notification email settings not found for userId: " + userId));

        return mapToDto(settings);
    }

    protected abstract void applySetting(NotificationEmailSettings settings, D dto);

    protected abstract R mapToDto(NotificationEmailSettings settings);

    protected void handleActivity(Long userId, boolean enabled) {
    }
}