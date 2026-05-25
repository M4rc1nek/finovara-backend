package com.finovara.corebackend.usersetting.notificationemail.action.accountdeleted.controller;

import com.finovara.corebackend.security.SecurityUtils;
import com.finovara.corebackend.usersetting.notificationemail.action.accountdeleted.service.NotifyOnAccountDeletedService;
import com.finovara.corebackend.usersetting.notificationemail.dto.NotificationEmailDto;
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
        notifyOnAccountDeletedService.saveEmailNotification(SecurityUtils.getCurrentUserId(), dto);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<NotificationEmailDto> getNotifyAccountDeleted() {
        return ResponseEntity.ok(notifyOnAccountDeletedService.getEmailNotification(SecurityUtils.getCurrentUserId()));
    }
}
