package com.finovara.notificationservice.notificationemail.service.settings.action.passwordchange.controller;

import com.finovara.notificationservice.notificationemail.dto.NotificationEmailDto;
import com.finovara.notificationservice.notificationemail.service.settings.action.passwordchange.service.NotifyPasswordChangeServiceAction;
import com.finovara.notificationservice.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notification-settings/notify-password-change")
@RequiredArgsConstructor
public class NotifyPasswordChangeController {

    private final NotifyPasswordChangeServiceAction notifyPasswordChangeService;

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
