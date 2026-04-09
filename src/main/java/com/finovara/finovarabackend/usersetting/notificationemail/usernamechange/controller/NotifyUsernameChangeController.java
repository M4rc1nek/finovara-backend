package com.finovara.finovarabackend.usersetting.notificationemail.usernamechange.controller;

import com.finovara.finovarabackend.security.SecurityUtils;
import com.finovara.finovarabackend.usersetting.notificationemail.dto.NotificationEmailDto;
import com.finovara.finovarabackend.usersetting.notificationemail.usernamechange.service.NotifyUsernameChangeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notification-settings/notify-username-change")
@RequiredArgsConstructor
public class NotifyUsernameChangeController {

    private final NotifyUsernameChangeService notifyUsernameChangeService;

    @PatchMapping
    public ResponseEntity<Void> saveNotifyUsernameChange(@RequestBody NotificationEmailDto dto) {
        notifyUsernameChangeService.saveEmailNotification(SecurityUtils.getCurrentUserEmail(), dto);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<NotificationEmailDto> getNotifyUsernameChange() {
        return ResponseEntity.ok(notifyUsernameChangeService.getEmailNotification(SecurityUtils.getCurrentUserEmail()));
    }
}
