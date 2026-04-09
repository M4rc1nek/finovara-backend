package com.finovara.finovarabackend.usersetting.notificationemail.passwordchange.service;

import com.finovara.finovarabackend.accountactivity.settings.model.SettingActivityStatus;
import com.finovara.finovarabackend.accountactivity.settings.model.SettingType;
import com.finovara.finovarabackend.accountactivity.settings.service.SettingsActivityService;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.usersetting.notificationemail.util.NotificationEmailSender;
import com.finovara.finovarabackend.usersetting.notificationemail.model.NotificationEmailSettings;
import com.finovara.finovarabackend.usersetting.notificationemail.passwordchange.dto.NotifyPasswordChangeDto;
import com.finovara.finovarabackend.util.user.accountmanagment.passwordpolicy.PasswordChangeEmailService;
import com.finovara.finovarabackend.util.user.service.UserManagerService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotifyPasswordChangeService {

    private final UserManagerService userManagerService;
    private final NotificationEmailSender notificationEmailSender;
    private final SettingsActivityService settingsActivityService;
    private final PasswordChangeEmailService passwordChangeEmailService;

    @Transactional
    public void saveNotifyOnPasswordChange(String email, NotifyPasswordChangeDto dto) {
        User user = userManagerService.getUserByEmailOrThrow(email);
        NotificationEmailSettings settings = user.getNotificationEmailSettings();

        settings.setNotifyOnPasswordChange(dto.notifyOnPasswordChange());
        if (settings.isNotifyOnPasswordChange()) {
            settingsActivityService.createSettingActivity(email, SettingActivityStatus.ENABLED, SettingType.NOTIFICATION_PASSWORD_CHANGED);
        } else {
            settingsActivityService.createSettingActivity(email, SettingActivityStatus.DISABLED, SettingType.NOTIFICATION_PASSWORD_CHANGED);
        }
    }

    public NotifyPasswordChangeDto getEmailOnPasswordChange(String email) {
        User user = userManagerService.getUserByEmailOrThrow(email);
        NotificationEmailSettings settings = user.getNotificationEmailSettings();

        return new NotifyPasswordChangeDto(
                settings.isNotifyOnPasswordChange()
        );
    }

    public void sendEmailOnPasswordChange(User user) {
        notificationEmailSender.sendIfEnabled(user, NotificationEmailSettings::isNotifyOnPasswordChange, passwordChangeEmailService::sendEmail);
    }
}
