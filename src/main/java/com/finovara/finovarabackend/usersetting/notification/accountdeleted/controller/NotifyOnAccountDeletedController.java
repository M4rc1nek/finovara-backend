package com.finovara.finovarabackend.usersetting.notification.accountdeleted.controller;

import com.finovara.finovarabackend.security.SecurityUtils;
import com.finovara.finovarabackend.usersetting.notification.accountdeleted.dto.NotifyOnAccountDeletedDto;
import com.finovara.finovarabackend.usersetting.notification.accountdeleted.service.NotifyOnAccountDeletedService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notification-settings/notify-account-deleted")
@RequiredArgsConstructor
public class NotifyOnAccountDeletedController {

    private final NotifyOnAccountDeletedService notifyOnAccountDeletedService;

    @PatchMapping
    public ResponseEntity<Void> saveNotifyAccountDeleted(@RequestBody NotifyOnAccountDeletedDto dto) {
        notifyOnAccountDeletedService.saveNotifyOnAccountDeleted(SecurityUtils.getCurrentUserEmail(), dto);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<NotifyOnAccountDeletedDto> getNotifyAccountDeleted() {
        return ResponseEntity.ok(notifyOnAccountDeletedService.getEmailOnAccountDeleted(SecurityUtils.getCurrentUserEmail()));
    }
}
