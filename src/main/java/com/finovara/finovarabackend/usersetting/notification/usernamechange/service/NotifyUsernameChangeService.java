package com.finovara.finovarabackend.usersetting.notification.usernamechange.service;

import com.finovara.finovarabackend.accountactivity.settings.model.SettingActivityStatus;
import com.finovara.finovarabackend.accountactivity.settings.model.SettingType;
import com.finovara.finovarabackend.accountactivity.settings.service.SettingsActivityService;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.usersetting.notification.model.NotificationSettings;
import com.finovara.finovarabackend.usersetting.notification.usernamechange.dto.NotifyUsernameChangeDto;
import com.finovara.finovarabackend.util.user.accountmanagment.usernamepolicy.UsernameChangeEmailService;
import com.finovara.finovarabackend.util.user.service.UserManagerService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotifyUsernameChangeService {

    private final UserManagerService userManagerService;
    private final UsernameChangeEmailService usernameChangeEmailService;
    private final SettingsActivityService settingsActivityService;

    @Transactional
    public void saveNotifyUsernameChange(String email, NotifyUsernameChangeDto dto) {
        User user = userManagerService.getUserByEmailOrThrow(email);
        NotificationSettings settings = user.getNotificationSettings();

        settings.setNotifyOnUsernameChange(dto.notifyOnUsernameChange());
        if (settings.isNotifyOnUsernameChange()) {
            settingsActivityService.createSettingActivity(email, SettingActivityStatus.ENABLED, SettingType.NOTIFICATION_USERNAME_CHANGED);
        } else {
            settingsActivityService.createSettingActivity(email, SettingActivityStatus.DISABLED, SettingType.NOTIFICATION_USERNAME_CHANGED);
        }

    }

    public NotifyUsernameChangeDto getEmailOnUsernameChange(String email) {
        User user = userManagerService.getUserByEmailOrThrow(email);
        NotificationSettings settings = user.getNotificationSettings();

        return new NotifyUsernameChangeDto(
                settings.isNotifyOnUsernameChange()
        );
    }

    public void sendEmailOnUsernameChange(User user) {
        NotificationSettings settings = user.getNotificationSettings();

        if (!settings.isNotifyOnUsernameChange()) return;

        usernameChangeEmailService.sendEmail(user);
    }
}
