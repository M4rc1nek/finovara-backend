package com.finovara.finovarabackend.usersetting.notification.accountdeleted.service;

import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.usersetting.notification.accountdeleted.dto.NotifyOnAccountDeletedDto;
import com.finovara.finovarabackend.usersetting.notification.model.NotificationSettings;
import com.finovara.finovarabackend.util.service.user.accountmanagment.accountpolicy.accountdeleted.AccountDeletedEmailService;
import com.finovara.finovarabackend.util.service.user.service.UserManagerService;
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

    private void sendEmailOnAccountDeleted(String email) {
        User user = userManagerService.getUserByEmailOrThrow(email);
        NotificationSettings settings = user.getNotificationSettings();

        if (!settings.isNotifyOnAccountDeleted()) return;

        accountDeletedEmailService.sendEmail(user);
    }
}
