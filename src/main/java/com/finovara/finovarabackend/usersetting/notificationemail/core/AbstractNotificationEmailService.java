package com.finovara.finovarabackend.usersetting.notificationemail.core;

import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.usersetting.notificationemail.dto.NotificationEmailDto;
import com.finovara.finovarabackend.usersetting.notificationemail.model.NotificationEmailSettings;
import com.finovara.finovarabackend.usersetting.notificationemail.util.NotificationEmailSender;
import com.finovara.finovarabackend.util.user.service.UserManagerService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public abstract class AbstractNotificationEmailService {
    protected final UserManagerService userManagerService;
    protected final NotificationEmailSender notificationEmailSender;

    @Transactional
    public void saveEmailNotification(Long userId, NotificationEmailDto dto) {
        User user = userManagerService.getUserByIdOrThrow(userId);
        NotificationEmailSettings settings = user.getNotificationEmailSettings();

        boolean enabled = isEnabled(dto);
        applySetting(settings, enabled);

        handleActivity(userId, enabled);
    }

    public NotificationEmailDto getEmailNotification(Long userId) {
        User user = userManagerService.getUserByIdOrThrow(userId);
        NotificationEmailSettings settings = user.getNotificationEmailSettings();

        return mapToDto(settings);
    }

    public void sendEmail(User user) {
        notificationEmailSender.sendIfEnabled(
                user,
                this::isNotificationEmailSettingsEnabled,
                this::sendEmailToUser
        );
    }

    protected abstract boolean isEnabled(NotificationEmailDto dto);

    protected abstract void applySetting(NotificationEmailSettings settings, boolean value);

    protected abstract boolean isNotificationEmailSettingsEnabled(NotificationEmailSettings settings);

    protected abstract NotificationEmailDto mapToDto(NotificationEmailSettings settings);

    protected abstract void sendEmailToUser(User user);

    protected void handleActivity(Long userId, boolean enabled) {
    }
}
