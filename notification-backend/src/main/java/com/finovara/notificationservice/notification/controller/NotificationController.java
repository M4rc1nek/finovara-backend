package com.finovara.notificationservice.notification.controller;

import com.finovara.contracts.model.SortType;
import com.finovara.notificationservice.notification.dto.NotificationResponse;
import com.finovara.notificationservice.notification.service.NotificationPersistenceService;
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
    public ResponseEntity<List<NotificationResponse>> getAllNotifications(@PathVariable Long userId, @RequestParam(defaultValue = "NEWEST") SortType sort) {
        return ResponseEntity.ok(notificationPersistenceService.getUserNotifications(userId, sort));
    }
}