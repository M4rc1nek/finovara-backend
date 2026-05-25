package com.finovara.corebackend.notification.processor;

import com.finovara.corebackend.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationProcessor {
    private final NotificationRepository notificationRepository;

    public void deleteNotifications(){
        notificationRepository.deleteAllInBatch();
    }
}
