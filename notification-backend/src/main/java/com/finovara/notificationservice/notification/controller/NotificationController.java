package com.finovara.notificationservice.notification.controller;

import com.finovara.contracts.util.SortType;
import com.finovara.notificationservice.notification.dto.NotificationResponse;
import com.finovara.notificationservice.notification.NotificationPersistenceService;
import com.finovara.notificationservice.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notifications")
public class NotificationController {
    private final NotificationPersistenceService notificationPersistenceService;

    @GetMapping
    public ResponseEntity<List<NotificationResponse>> getAllNotifications(@RequestParam(defaultValue = "NEWEST") SortType sortType, @RequestParam(defaultValue = "0") int page) {
        return ResponseEntity.ok(notificationPersistenceService.getUserNotifications(SecurityUtils.getCurrentUserId(), sortType, page));
    }
}