package com.finovara.notificationservice.notification.scheduler;

import com.finovara.notificationservice.notification.processor.NotificationProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class NotificationScheduler {
    private final NotificationProcessor notificationProcessor;

    @Scheduled(cron = "${scheduler.notification.delete-cron}", zone = "Europe/Warsaw")
    @SchedulerLock(name = "deleteNotifications", lockAtMostFor = "10m", lockAtLeastFor = "30s")
    public void deleteNotifications(){
        notificationProcessor.deleteNotifications();
        log.info("Notifications deleted");

    }
}
