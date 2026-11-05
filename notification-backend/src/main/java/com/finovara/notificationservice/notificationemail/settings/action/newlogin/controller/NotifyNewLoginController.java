package com.finovara.notificationservice.notificationemail.settings.action.newlogin.controller;

import com.finovara.notificationservice.notificationemail.dto.NotificationEmailDto;
import com.finovara.notificationservice.notificationemail.settings.action.newlogin.service.NotifyNewLoginServiceAction;
import com.finovara.notificationservice.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notification-settings/notify-new-login")
@RequiredArgsConstructor
public class NotifyNewLoginController {

    private final NotifyNewLoginServiceAction notifyNewLoginService;

    @PatchMapping
    public ResponseEntity<Void> saveNotifyNewLogin(@RequestBody NotificationEmailDto dto) {
        notifyNewLoginService.saveEmailNotification(SecurityUtils.getCurrentUserId(), dto);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<NotificationEmailDto> getNotifyNewLogin() {
        return ResponseEntity.ok(notifyNewLoginService.getEmailNotification(SecurityUtils.getCurrentUserId()));
    }
}
