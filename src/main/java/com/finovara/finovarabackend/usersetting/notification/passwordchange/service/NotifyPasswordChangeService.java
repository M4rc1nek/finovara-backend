package com.finovara.finovarabackend.usersetting.notification.passwordchange.service;

import com.finovara.finovarabackend.accountactivity.settings.model.SettingActivityStatus;
import com.finovara.finovarabackend.accountactivity.settings.model.SettingType;
import com.finovara.finovarabackend.accountactivity.settings.service.SettingsActivityService;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.util.service.user.accountmanagment.passwordpolicy.PasswordChangeEmailService;
import com.finovara.finovarabackend.usersetting.notification.model.NotificationSettings;
import com.finovara.finovarabackend.usersetting.notification.passwordchange.dto.NotifyPasswordChangeDto;
import com.finovara.finovarabackend.util.service.user.service.UserManagerService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotifyPasswordChangeService {

    private final UserManagerService userManagerService;
    private final PasswordChangeEmailService passwordChangeEmailService;
    private final SettingsActivityService settingsActivityService;

    @Transactional
    public void saveNotifyOnPasswordChange(String email, NotifyPasswordChangeDto dto) {
        User user = userManagerService.getUserByEmailOrThrow(email);
        NotificationSettings settings = user.getNotificationSettings();

        settings.setNotifyOnPasswordChange(dto.notifyOnPasswordChange());
        if (settings.isNotifyOnPasswordChange()) {
            settingsActivityService.createSettingActivity(email, SettingActivityStatus.ENABLED, SettingType.NOTIFICATION_PASSWORD_CHANGED);
        } else {
            settingsActivityService.createSettingActivity(email, SettingActivityStatus.DISABLED, SettingType.NOTIFICATION_PASSWORD_CHANGED);
        }
    }

    public NotifyPasswordChangeDto getEmailOnPasswordChange(String email) {
        User user = userManagerService.getUserByEmailOrThrow(email);
        NotificationSettings settings = user.getNotificationSettings();

        return new NotifyPasswordChangeDto(
                settings.isNotifyOnPasswordChange()
        );
    }

    private void sendEmailOnPasswordChange(String email) {
        User user = userManagerService.getUserByEmailOrThrow(email);
        NotificationSettings settings = user.getNotificationSettings();

        if (!settings.isNotifyOnPasswordChange()) return;

        passwordChangeEmailService.sendEmail(user);
    }
}
