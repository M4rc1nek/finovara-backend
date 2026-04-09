package com.finovara.finovarabackend.usersetting.notificationemail.core;

import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.usersetting.notificationemail.model.NotificationEmailSettings;
import com.finovara.finovarabackend.usersetting.notificationemail.util.NotificationEmailSender;
import com.finovara.finovarabackend.util.user.service.UserManagerService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public abstract class AbstractNotificationEmailService<DTO> {
    protected final UserManagerService userManagerService;
    protected final NotificationEmailSender notificationEmailSender;

    @Transactional
    public void saveEmailNotification(String email, DTO dto) {
        User user = userManagerService.getUserByEmailOrThrow(email);
        NotificationEmailSettings settings = user.getNotificationEmailSettings();

        boolean enabled = isEnabled(dto);
        applySetting(settings, enabled);

        handleActivity(email, enabled);
    }

    public DTO getEmailNotification(String email) {
        User user = userManagerService.getUserByEmailOrThrow(email);
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

    protected abstract boolean isEnabled(DTO dto);

    protected abstract void applySetting(NotificationEmailSettings settings, boolean value);

    protected abstract boolean isNotificationEmailSettingsEnabled(NotificationEmailSettings settings);

    protected abstract DTO mapToDto(NotificationEmailSettings settings);

    protected abstract void sendEmailToUser(User user);

    protected void handleActivity(String email, boolean enabled) {
    }
}
