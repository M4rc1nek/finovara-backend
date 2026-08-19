package com.finovara.notificationservice.notificationemail.service.action.passwordchange.controller;

import com.finovara.notificationservice.notificationemail.dto.NotificationEmailDto;
import com.finovara.notificationservice.notificationemail.service.action.passwordchange.service.NotifyPasswordChangeService;
import com.finovara.notificationservice.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notification-settings/notify-password-change")
@RequiredArgsConstructor
public class NotifyPasswordChangeController {

    private final NotifyPasswordChangeService notifyPasswordChangeService;

    @PatchMapping
    public ResponseEntity<Void> saveNotifyPasswordChange(@RequestBody NotificationEmailDto dto) {
        notifyPasswordChangeService.saveEmailNotification(SecurityUtils.getCurrentUserId(), dto);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<NotificationEmailDto> getNotifyPasswordChange() {
        return ResponseEntity.ok(notifyPasswordChangeService.getEmailNotification(SecurityUtils.getCurrentUserId()));
    }
}
