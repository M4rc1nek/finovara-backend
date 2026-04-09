package com.finovara.finovarabackend.usersetting.notificationemail.accountdeleted.service;

import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.usersetting.notificationemail.dto.NotificationEmailDto;
import com.finovara.finovarabackend.usersetting.notificationemail.util.NotificationEmailSender;
import com.finovara.finovarabackend.usersetting.notificationemail.model.NotificationEmailSettings;
import com.finovara.finovarabackend.util.user.accountmanagment.accountpolicy.accountdeleted.AccountDeletedEmailService;
import com.finovara.finovarabackend.util.user.service.UserManagerService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotifyOnAccountDeletedService {
    private final UserManagerService userManagerService;
    private final AccountDeletedEmailService accountDeletedEmailService;
    private final NotificationEmailSender notificationEmailSender;

    @Transactional
    public void saveNotifyOnAccountDeleted(String email, NotificationEmailDto dto) {
        User user = userManagerService.getUserByEmailOrThrow(email);
        NotificationEmailSettings settings = user.getNotificationEmailSettings();

        settings.setNotifyOnAccountDeleted(dto.enabled());

    }

    public NotificationEmailDto getEmailOnAccountDeleted(String email) {
        User user = userManagerService.getUserByEmailOrThrow(email);
        NotificationEmailSettings settings = user.getNotificationEmailSettings();

        return new NotificationEmailDto(
                settings.isNotifyOnAccountDeleted()
        );
    }

    public void sendEmailOnAccountDeleted(User user) {
       notificationEmailSender.sendIfEnabled(user, NotificationEmailSettings::isNotifyOnAccountDeleted, accountDeletedEmailService::sendEmail);
    }

}
