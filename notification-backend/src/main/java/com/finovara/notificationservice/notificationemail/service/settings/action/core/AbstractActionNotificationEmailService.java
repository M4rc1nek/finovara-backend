package com.finovara.notificationservice.notificationemail.service.settings.action.core;

import com.finovara.contracts.authorization.additionalcode.resolver.AdditionalAuthorizationCodeResolver;
import com.finovara.contracts.exception.notfound.RequestedEntityNotFoundException;
import com.finovara.notificationservice.notificationemail.dto.NotificationEmailDto;
import com.finovara.notificationservice.notificationemail.dto.UserEmailDataDto;
import com.finovara.notificationservice.notificationemail.model.NotificationEmailSettings;
import com.finovara.notificationservice.notificationemail.repository.NotificationEmailSettingsRepository;
import com.finovara.notificationservice.notificationemail.service.settings.NotificationSettingEmailSender;
import com.finovara.notificationservice.feignclient.AuthBackendClient;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public abstract class AbstractNotificationEmailService {
    protected final NotificationEmailSettingsRepository notificationEmailSettingsRepository;
    protected final NotificationSettingEmailSender notificationSettingEmailSender;
    protected final AuthBackendClient authBackendClient;
    protected final AdditionalAuthorizationCodeResolver additionalAuthorizationCodeResolver;

    @Transactional
    public void saveEmailNotification(Long userId, NotificationEmailDto dto) {
        authBackendClient.confirmAuthorizationCode(userId, additionalAuthorizationCodeResolver.resolve(dto.authorizationCode()));
        NotificationEmailSettings settings = notificationEmailSettingsRepository.findByUserId(userId)
                .orElseThrow(() -> new RequestedEntityNotFoundException("Notification email settings not found for userId: " + userId));

        boolean enabled = isEnabled(dto);
        applySetting(settings, enabled);
        notificationEmailSettingsRepository.save(settings);

        handleActivity(userId, enabled);
    }

    public NotificationEmailDto getEmailNotification(Long userId) {
        NotificationEmailSettings settings = notificationEmailSettingsRepository.findByUserId(userId)
                .orElseThrow(() -> new RequestedEntityNotFoundException("Notification email settings not found for userId: " + userId));

        return mapToDto(settings);
    }

    public void sendEmail(Long userId, UserEmailDataDto userEmailData) {
        notificationSettingEmailSender.sendIfEnabled(
                userId,
                userEmailData,
                this::isNotificationEmailSettingsEnabled,
                this::sendEmailToUser
        );
    }

    protected abstract boolean isEnabled(NotificationEmailDto dto);

    protected abstract void applySetting(NotificationEmailSettings settings, boolean value);

    protected abstract boolean isNotificationEmailSettingsEnabled(NotificationEmailSettings settings);

    protected abstract NotificationEmailDto mapToDto(NotificationEmailSettings settings);

    protected abstract void sendEmailToUser(Long userId, UserEmailDataDto userEmailData);

    protected void handleActivity(Long userId, boolean enabled) {
    }
}
