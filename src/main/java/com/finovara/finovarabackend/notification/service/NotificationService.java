package com.finovara.finovarabackend.notification.service;

import com.finovara.finovarabackend.notification.dto.NotificationDto;
import com.finovara.finovarabackend.notification.source.NotificationCreator;
import jakarta.transaction.Transactional;
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
        List<NotificationDto> result = new ArrayList<>();

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
