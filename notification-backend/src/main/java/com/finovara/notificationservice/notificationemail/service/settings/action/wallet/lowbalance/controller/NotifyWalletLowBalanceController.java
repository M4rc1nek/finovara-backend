package com.finovara.notificationservice.notificationemail.service.settings.action.usernamechange.controller;

import com.finovara.notificationservice.notificationemail.dto.NotificationEmailDto;
import com.finovara.notificationservice.notificationemail.service.settings.action.usernamechange.service.NotifyUsernameChangeServiceAction;
import com.finovara.notificationservice.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notification-settings/notify-username-change")
@RequiredArgsConstructor
public class NotifyUsernameChangeController {

    private final NotifyUsernameChangeServiceAction notifyUsernameChangeService;

    @PatchMapping
    public ResponseEntity<Void> saveNotifyUsernameChange(@RequestBody NotificationEmailDto dto) {
        notifyUsernameChangeService.saveEmailNotification(SecurityUtils.getCurrentUserId(), dto);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<NotificationEmailDto> getNotifyUsernameChange() {
        return ResponseEntity.ok(notifyUsernameChangeService.getEmailNotification(SecurityUtils.getCurrentUserId()));
    }
}
