package com.finovara.finovarabackend.usersetting.notificationemail.accountdeleted.service;

import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.usersetting.notificationemail.accountdeleted.dto.NotifyOnAccountDeletedDto;
import com.finovara.finovarabackend.usersetting.notificationemail.model.NotificationSettings;
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

    @Transactional
    public void saveNotifyOnAccountDeleted(String email, NotifyOnAccountDeletedDto dto) {
        User user = userManagerService.getUserByEmailOrThrow(email);
        NotificationSettings settings = user.getNotificationSettings();

        settings.setNotifyOnAccountDeleted(dto.notifyOnAccountDeleted());

    }

    public NotifyOnAccountDeletedDto getEmailOnAccountDeleted(String email) {
        User user = userManagerService.getUserByEmailOrThrow(email);
        NotificationSettings settings = user.getNotificationSettings();

        return new NotifyOnAccountDeletedDto(
                settings.isNotifyOnAccountDeleted()
        );
    }

    public void sendEmailOnAccountDeleted(User user) {
        NotificationSettings settings = user.getNotificationSettings();

        if (!settings.isNotifyOnAccountDeleted()) return;

        accountDeletedEmailService.sendEmail(user);
    }
}
