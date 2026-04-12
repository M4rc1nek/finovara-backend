package com.finovara.finovarabackend.notification.controller;

import com.finovara.finovarabackend.notification.dto.NotificationResponse;
import com.finovara.finovarabackend.notification.service.NotificationPersistenceService;
import com.finovara.finovarabackend.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notifications")
public class NotificationController {
    private final NotificationService notificationService;
    private final NotificationPersistenceService notificationPersistenceService;

    @PostMapping("/{userId}")
    public ResponseEntity<Void> createNotification(@PathVariable Long userId) {
        notificationService.createNotifications(userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{userId}")
    public ResponseEntity<List<NotificationResponse>> getAllNotifications(@PathVariable Long userId) {
        return ResponseEntity.ok(notificationPersistenceService.getUserNotifications(userId));
    }

}
