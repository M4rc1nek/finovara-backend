package com.finovara.finovarabackend.usersetting.notificationemail.action.usernamechange.service;

import com.finovara.finovarabackend.accountactivity.settings.model.SettingActivityStatus;
import com.finovara.finovarabackend.accountactivity.settings.model.SettingType;
import com.finovara.finovarabackend.accountactivity.settings.service.SettingsActivityService;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.usersetting.notificationemail.core.AbstractNotificationEmailService;
import com.finovara.finovarabackend.usersetting.notificationemail.model.NotificationEmailSettings;
import com.finovara.finovarabackend.usersetting.notificationemail.dto.NotificationEmailDto;
import com.finovara.finovarabackend.usersetting.notificationemail.util.NotificationEmailSender;
import com.finovara.finovarabackend.usersetting.notificationemail.util.emailsender.UsernameChangeNotifier;
import com.finovara.finovarabackend.util.user.service.UserManagerService;
import org.springframework.stereotype.Service;

@Service
public class NotifyUsernameChangeService extends AbstractNotificationEmailService {

    private final UsernameChangeNotifier usernameChangeNotifier;
    private final SettingsActivityService settingsActivityService;

    public NotifyUsernameChangeService(UserManagerService userManagerService, NotificationEmailSender notificationEmailSender,
                                       UsernameChangeNotifier usernameChangeNotifier,
                                       SettingsActivityService settingsActivityService) {
        super(userManagerService, notificationEmailSender);
        this.usernameChangeNotifier = usernameChangeNotifier;
        this.settingsActivityService = settingsActivityService;
    }

    @Override
    protected boolean isEnabled(NotificationEmailDto dto) {
        return dto.enabled();
    }

    @Override
    protected void applySetting(NotificationEmailSettings settings, boolean value) {
        settings.setNotifyOnUsernameChange(value);
    }

    @Override
    protected boolean  isNotificationEmailSettingsEnabled(NotificationEmailSettings settings) {
        return settings.isNotifyOnUsernameChange();
    }

    @Override
    protected NotificationEmailDto mapToDto(NotificationEmailSettings settings) {
        return new NotificationEmailDto(settings.isNotifyOnUsernameChange());
    }

    @Override
    protected void sendEmailToUser(User user){
        usernameChangeNotifier.sendEmail(user);
    }

    @Override
    protected void handleActivity(Long userId, boolean enabled) {
        settingsActivityService.createSettingActivity(
                userId,
                enabled ? SettingActivityStatus.ENABLED : SettingActivityStatus.DISABLED,
                SettingType.NOTIFICATION_USERNAME_CHANGED
        );
    }
}
