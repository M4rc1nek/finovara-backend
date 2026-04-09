package com.finovara.finovarabackend.usersetting.notificationemail.accountdeleted.service;

import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.usersetting.notificationemail.core.AbstractNotificationEmailService;
import com.finovara.finovarabackend.usersetting.notificationemail.dto.NotificationEmailDto;
import com.finovara.finovarabackend.usersetting.notificationemail.model.NotificationEmailSettings;
import com.finovara.finovarabackend.usersetting.notificationemail.util.NotificationEmailSender;
import com.finovara.finovarabackend.util.user.accountmanagment.accountpolicy.accountdeleted.AccountDeletedEmailService;
import com.finovara.finovarabackend.util.user.service.UserManagerService;
import org.springframework.stereotype.Service;

@Service
public class NotifyOnAccountDeletedService extends AbstractNotificationEmailService {
    private final AccountDeletedEmailService accountDeletedEmailService;

    public NotifyOnAccountDeletedService(UserManagerService userManagerService, NotificationEmailSender notificationEmailSender,
                                         AccountDeletedEmailService accountDeletedEmailService) {
        super(userManagerService, notificationEmailSender);
        this.accountDeletedEmailService = accountDeletedEmailService;
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
        accountDeletedEmailService.sendEmail(user);
    }
}
