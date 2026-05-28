package com.finovara.corebackend.usersetting.notificationemail.action.emailchange.service;

import com.finovara.contracts.event.settings.SettingsActivityEvent;
import com.finovara.contracts.model.activity.SettingActivityStatus;
import com.finovara.contracts.model.activity.SettingType;
import com.finovara.corebackend.user.model.User;
import com.finovara.corebackend.usersetting.notificationemail.core.AbstractNotificationEmailService;
import com.finovara.corebackend.usersetting.notificationemail.dto.NotificationEmailDto;
import com.finovara.corebackend.usersetting.notificationemail.model.NotificationEmailSettings;
import com.finovara.corebackend.usersetting.notificationemail.util.NotificationEmailSender;
import com.finovara.corebackend.usersetting.notificationemail.util.emailsender.EmailChangeNotifier;
import com.finovara.corebackend.util.user.service.UserManagerService;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class NotifyEmailChangeService extends AbstractNotificationEmailService {
    private final EmailChangeNotifier emailChangeNotifier;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public NotifyEmailChangeService(UserManagerService userManagerService, NotificationEmailSender notificationEmailSender, EmailChangeNotifier emailChangeNotifier, KafkaTemplate<String, Object> kafkaTemplate) {
        super(userManagerService, notificationEmailSender);
        this.emailChangeNotifier = emailChangeNotifier;
        this.kafkaTemplate = kafkaTemplate;
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
        kafkaTemplate.send("activity.settings", new SettingsActivityEvent(userId, SettingType.NOTIFICATION_EMAIL_CHANGED, enabled ? SettingActivityStatus.ENABLED : SettingActivityStatus.DISABLED, LocalDateTime.now()));
    }
}
