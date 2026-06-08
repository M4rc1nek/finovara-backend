package com.finovara.corebackend.notification.controller;

import com.finovara.corebackend.notification.dto.NotificationResponse;
import com.finovara.corebackend.notification.service.NotificationPersistenceService;
import com.finovara.contracts.model.SortType;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notifications")
public class NotificationController {
    private final NotificationPersistenceService notificationPersistenceService;

    @GetMapping("/{userId}")
    public ResponseEntity<List<NotificationResponse>> getAllNotifications(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "NEWEST") SortType sort) {
        return ResponseEntity.ok(notificationPersistenceService.getUserNotifications(userId, sort));
    }
}