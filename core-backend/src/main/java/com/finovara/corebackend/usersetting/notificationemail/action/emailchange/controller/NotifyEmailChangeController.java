package com.finovara.corebackend.usersetting.notificationemail.action.emailchange.controller;

import com.finovara.corebackend.security.SecurityUtils;
import com.finovara.corebackend.usersetting.notificationemail.dto.NotificationEmailDto;
import com.finovara.corebackend.usersetting.notificationemail.action.emailchange.service.NotifyEmailChangeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notification-settings/notify-email-change")
@RequiredArgsConstructor
public class NotifyEmailChangeController {
    private final NotifyEmailChangeService notifyEmailChangeService;

    @PatchMapping
    public ResponseEntity<Void> saveNotifyEmailChange(@RequestBody NotificationEmailDto dto) {
        notifyEmailChangeService.saveEmailNotification(SecurityUtils.getCurrentUserId(), dto);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<NotificationEmailDto> getNotifyEmailChange() {
        return ResponseEntity.ok(notifyEmailChangeService.getEmailNotification(SecurityUtils.getCurrentUserId()));
    }
}
