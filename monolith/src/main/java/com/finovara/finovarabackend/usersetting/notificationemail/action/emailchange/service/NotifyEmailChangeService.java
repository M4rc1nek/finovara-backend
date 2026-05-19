package com.finovara.finovarabackend.usersetting.notificationemail.action.emailchange.service;

import com.finovara.finovarabackend.accountactivity.settings.model.SettingActivityStatus;
import com.finovara.finovarabackend.accountactivity.settings.model.SettingType;
import com.finovara.finovarabackend.accountactivity.settings.service.SettingsActivityService;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.usersetting.notificationemail.core.AbstractNotificationEmailService;
import com.finovara.finovarabackend.usersetting.notificationemail.dto.NotificationEmailDto;
import com.finovara.finovarabackend.usersetting.notificationemail.model.NotificationEmailSettings;
import com.finovara.finovarabackend.usersetting.notificationemail.util.NotificationEmailSender;
import com.finovara.finovarabackend.usersetting.notificationemail.util.emailsender.EmailChangeNotifier;
import com.finovara.finovarabackend.util.user.service.UserManagerService;
import org.springframework.stereotype.Service;

@Service
public class NotifyEmailChangeService extends AbstractNotificationEmailService {
    private final EmailChangeNotifier emailChangeNotifier;
    private final SettingsActivityService settingsActivityService;

    public NotifyEmailChangeService(UserManagerService userManagerService, NotificationEmailSender notificationEmailSender,
                                    EmailChangeNotifier emailChangeNotifier,
                                    SettingsActivityService settingsActivityService) {
        super(userManagerService, notificationEmailSender);
        this.emailChangeNotifier = emailChangeNotifier;
        this.settingsActivityService = settingsActivityService;
    }

    @Override
    protected boolean isEnabled(NotificationEmailDto dto) {
        return dto.enabled();
    }

    @Override
    protected void applySetting(NotificationEmailSettings settings, boolean value) {
        settings.setNotifyOnEmailChange(value);
    }

    @Override
    protected boolean isNotificationEmailSettingsEnabled(NotificationEmailSettings settings) {
        return settings.isNotifyOnEmailChange();
    }

    @Override
    protected NotificationEmailDto mapToDto(NotificationEmailSettings settings) {
        return new NotificationEmailDto(settings.isNotifyOnEmailChange());

    }

    @Override
    protected void sendEmailToUser(User user) {
        emailChangeNotifier.sendEmail(user);
    }

    @Override
    protected void handleActivity(Long userId, boolean enabled) {
        settingsActivityService.createSettingActivity(
                userId,
                enabled ? SettingActivityStatus.ENABLED : SettingActivityStatus.DISABLED,
                SettingType.NOTIFICATION_EMAIL_CHANGED
        );
    }
}
