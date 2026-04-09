package com.finovara.finovarabackend.usersetting.notificationemail.passwordchange.controller;

import com.finovara.finovarabackend.security.SecurityUtils;
import com.finovara.finovarabackend.usersetting.notificationemail.dto.NotificationEmailDto;
import com.finovara.finovarabackend.usersetting.notificationemail.passwordchange.service.NotifyPasswordChangeService;
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
        notifyPasswordChangeService.saveNotifyOnPasswordChange(SecurityUtils.getCurrentUserEmail(), dto);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<NotificationEmailDto> getNotifyPasswordChange() {
        return ResponseEntity.ok(notifyPasswordChangeService.getEmailOnPasswordChange(SecurityUtils.getCurrentUserEmail()));
    }
}
