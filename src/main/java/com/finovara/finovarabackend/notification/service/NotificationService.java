package com.finovara.finovarabackend.notification.service;

import com.finovara.finovarabackend.notification.dto.NotificationDto;
import com.finovara.finovarabackend.notification.source.NotificationSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {
    private final List<NotificationSource> sources;

    public List<NotificationDto> fetchAllNotifications(Long userId) {
        List<NotificationDto> result = new ArrayList<>();

        for (NotificationSource source : sources) {
            try {
                result.addAll(source.getNotifications(userId));
            } catch (Exception e) {
                log.error("Notification source failed: {}", source.getClass().getSimpleName(), e);
            }
        }

        return result;
    }
}
