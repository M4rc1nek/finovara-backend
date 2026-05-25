package com.finovara.corebackend.usersetting.notificationemail.action.accountdeleted.service;

import com.finovara.activityservice.contracts.event.settings.SettingsActivityEvent;
import com.finovara.activityservice.contracts.model.activity.SettingActivityStatus;
import com.finovara.activityservice.contracts.model.activity.SettingType;
import com.finovara.corebackend.user.model.User;
import com.finovara.corebackend.usersetting.notificationemail.core.AbstractNotificationEmailService;
import com.finovara.corebackend.usersetting.notificationemail.dto.NotificationEmailDto;
import com.finovara.corebackend.usersetting.notificationemail.model.NotificationEmailSettings;
import com.finovara.corebackend.usersetting.notificationemail.util.NotificationEmailSender;
import com.finovara.corebackend.usersetting.notificationemail.util.emailsender.AccountDeletedNotifier;
import com.finovara.corebackend.util.user.service.UserManagerService;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class NotifyOnAccountDeletedService extends AbstractNotificationEmailService {
    private final AccountDeletedNotifier accountDeletedNotifier;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public NotifyOnAccountDeletedService(UserManagerService userManagerService, NotificationEmailSender notificationEmailSender, KafkaTemplate<String, Object> kafkaTemplate, AccountDeletedNotifier accountDeletedNotifier) {
        super(userManagerService, notificationEmailSender);
        this.accountDeletedNotifier = accountDeletedNotifier;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    protected boolean isEnabled(NotificationEmailDto dto) {
        return dto.enabled();
    }

    @Override
    protected void applySetting(NotificationEmailSettings settings, boolean value) {
        settings.setNotifyOnAccountDeleted(value);
    }

    @Override
    protected boolean isNotificationEmailSettingsEnabled(NotificationEmailSettings settings) {
        return settings.isNotifyOnAccountDeleted();
    }

    @Override
    protected NotificationEmailDto mapToDto(NotificationEmailSettings settings) {
        return new NotificationEmailDto(settings.isNotifyOnAccountDeleted());
    }

    @Override
    protected void sendEmailToUser(User user) {
        accountDeletedNotifier.sendEmail(user);
    }

    @Override
    protected void handleActivity(Long userId, boolean enabled) {
        kafkaTemplate.send("activity.settings", new SettingsActivityEvent(userId, SettingType.NOTIFICATION_ACCOUNT_DELETED, enabled ? SettingActivityStatus.ENABLED : SettingActivityStatus.DISABLED, LocalDateTime.now()));
    }
}
