package com.finovara.finovarabackend.usersetting.notificationemail.action.passwordchange.service;

import com.finovara.finovarabackend.accountactivity.settings.model.SettingActivityStatus;
import com.finovara.finovarabackend.accountactivity.settings.model.SettingType;
import com.finovara.finovarabackend.accountactivity.settings.service.SettingsActivityService;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.usersetting.notificationemail.core.AbstractNotificationEmailService;
import com.finovara.finovarabackend.usersetting.notificationemail.dto.NotificationEmailDto;
import com.finovara.finovarabackend.usersetting.notificationemail.model.NotificationEmailSettings;
import com.finovara.finovarabackend.usersetting.notificationemail.util.NotificationEmailSender;
import com.finovara.finovarabackend.usersetting.notificationemail.util.emailsender.PasswordChangeNotifier;
import com.finovara.finovarabackend.util.user.service.UserManagerService;
import org.springframework.stereotype.Service;

@Service
public class NotifyPasswordChangeService extends AbstractNotificationEmailService {

    private final PasswordChangeNotifier passwordChangeNotifier;
    private final SettingsActivityService settingsActivityService;

    public NotifyPasswordChangeService(UserManagerService userManagerService, NotificationEmailSender notificationEmailSender,
                                       PasswordChangeNotifier passwordChangeNotifier,
                                       SettingsActivityService settingsActivityService) {
        super(userManagerService, notificationEmailSender);
        this.passwordChangeNotifier = passwordChangeNotifier;
        this.settingsActivityService = settingsActivityService;
    }

    @Override
    protected boolean isEnabled(NotificationEmailDto dto) {
        return dto.enabled();
    }

    @Override
    protected void applySetting(NotificationEmailSettings settings, boolean value) {
        settings.setNotifyOnPasswordChange(value);
    }

    @Override
    protected boolean isNotificationEmailSettingsEnabled(NotificationEmailSettings settings) {
        return settings.isNotifyOnPasswordChange();
    }

    @Override
    protected NotificationEmailDto mapToDto(NotificationEmailSettings settings) {
        return new NotificationEmailDto(settings.isNotifyOnPasswordChange());

    }

    @Override
    protected void sendEmailToUser(User user) {
        passwordChangeNotifier.sendEmail(user);
    }

    @Override
    protected void handleActivity(Long userId, boolean enabled) {
        settingsActivityService.createSettingActivity(
                userId,
                enabled ? SettingActivityStatus.ENABLED : SettingActivityStatus.DISABLED,
                SettingType.NOTIFICATION_PASSWORD_CHANGED
        );
    }
}
