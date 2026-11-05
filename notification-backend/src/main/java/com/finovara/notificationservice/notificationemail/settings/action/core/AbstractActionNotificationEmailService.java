package com.finovara.notificationservice.notificationemail.settings.action.core;

import com.finovara.contracts.user.authorization.additionalcode.resolver.AdditionalAuthorizationCodeResolver;
import com.finovara.contracts.exception.notfound.RequestedEntityNotFoundException;
import com.finovara.contracts.mainaccount.activity.event.settings.SettingsActivityEvent;
import com.finovara.contracts.mainaccount.activity.model.SettingActivityStatus;
import com.finovara.contracts.mainaccount.activity.model.SettingType;
import com.finovara.contracts.outbox.OutboxService;
import com.finovara.notificationservice.notificationemail.model.EmailNotificationSettingRequest;
import com.finovara.notificationservice.notificationemail.model.NotificationEmailSettings;
import com.finovara.notificationservice.notificationemail.settings.repository.NotificationEmailSettingsRepository;
import com.finovara.notificationservice.feignclient.AuthBackendClient;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@RequiredArgsConstructor
public abstract class AbstractActionNotificationEmailService<D extends EmailNotificationSettingRequest, R> {

    protected final NotificationEmailSettingsRepository notificationEmailSettingsRepository;
    protected final AuthBackendClient authBackendClient;
    protected final AdditionalAuthorizationCodeResolver additionalAuthorizationCodeResolver;
    private final OutboxService outboxService;

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

    protected abstract void handleActivity(Long userId, boolean enabled);

    protected final void publishActivity(Long userId, SettingType settingType, boolean enabled) {
        outboxService.save("NotificationSettings", userId.toString(), "settings.changed",
                new SettingsActivityEvent(userId, settingType,
                        enabled ? SettingActivityStatus.ENABLED : SettingActivityStatus.DISABLED,
                        LocalDateTime.now()));
    }
}