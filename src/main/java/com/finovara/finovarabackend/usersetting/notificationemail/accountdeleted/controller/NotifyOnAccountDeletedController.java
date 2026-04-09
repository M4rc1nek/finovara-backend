package com.finovara.finovarabackend.usersetting.notificationemail.accountdeleted.controller;

import com.finovara.finovarabackend.notification.model.Notification;
import com.finovara.finovarabackend.security.SecurityUtils;
import com.finovara.finovarabackend.usersetting.notificationemail.accountdeleted.dto.NotifyOnAccountDeletedDto;
import com.finovara.finovarabackend.usersetting.notificationemail.accountdeleted.service.NotifyOnAccountDeletedService;
import com.finovara.finovarabackend.usersetting.notificationemail.usernamechange.dto.NotificationEmailDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notification-settings/notify-account-deleted")
@RequiredArgsConstructor
public class NotifyOnAccountDeletedController {

    private final NotifyOnAccountDeletedService notifyOnAccountDeletedService;

    @PatchMapping
    public ResponseEntity<Void> saveNotifyAccountDeleted(@RequestBody NotificationEmailDto dto) {
        notifyOnAccountDeletedService.saveNotifyOnAccountDeleted(SecurityUtils.getCurrentUserEmail(), dto);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<NotificationEmailDto> getNotifyAccountDeleted() {
        return ResponseEntity.ok(notifyOnAccountDeletedService.getEmailOnAccountDeleted(SecurityUtils.getCurrentUserEmail()));
    }
}
