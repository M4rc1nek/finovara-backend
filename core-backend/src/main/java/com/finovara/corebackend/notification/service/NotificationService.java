package com.finovara.corebackend.notification.service;

import com.finovara.corebackend.notification.dto.NotificationResponse;
import com.finovara.corebackend.notification.source.NotificationCreator;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {
    private final List<NotificationCreator> sources;
    private final NotificationPersistenceService notificationPersistenceService;

    @Transactional
    public void createNotifications(Long userId) {
        List<NotificationResponse> result = new ArrayList<>();

        for (NotificationCreator source : sources) {
            try {
                result.addAll(source.getNotifications(userId));
            } catch (Exception e) {
                log.error("Notification source failed: {}", source.getClass().getSimpleName(), e);
            }
        }
        notificationPersistenceService.saveAll(userId, result);
    }
}
