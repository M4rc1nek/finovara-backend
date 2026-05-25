package com.finovara.corebackend.notification.controller;

import com.finovara.corebackend.notification.dto.NotificationResponse;
import com.finovara.corebackend.notification.service.NotificationPersistenceService;
import com.finovara.corebackend.notification.service.NotificationService;
import com.finovara.activityservice.contracts.model.SortType;
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
    public ResponseEntity<List<NotificationResponse>> getAllNotifications(@PathVariable Long userId, @RequestParam(defaultValue = "NEWEST") SortType sort) {
        return ResponseEntity.ok(notificationPersistenceService.getUserNotifications(userId, sort));
    }

}
