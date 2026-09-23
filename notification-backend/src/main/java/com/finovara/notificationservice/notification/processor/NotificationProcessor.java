package com.finovara.notificationservice.notification.processor;

import com.finovara.notificationservice.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class NotificationProcessor {
    private final NotificationRepository notificationRepository;

    public void deleteNotifications(){
        notificationRepository.deleteAllInBatch();
        log.info("Notifications has been deleted.");
    }
}
