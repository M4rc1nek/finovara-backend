package com.finovara.finovarabackend.notification.processor;

import com.finovara.finovarabackend.notification.repository.NotificationRepository;
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
