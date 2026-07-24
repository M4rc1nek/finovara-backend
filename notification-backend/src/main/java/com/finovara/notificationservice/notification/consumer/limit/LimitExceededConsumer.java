package com.finovara.notificationservice.notification.consumer.limit;

import com.finovara.contracts.event.notification.limit.LimitStatsEvent;
import com.finovara.contracts.model.NotificationType;
import com.finovara.notificationservice.notification.dto.limit.LimitExceededDto;
import com.finovara.notificationservice.notification.NotificationPersistenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@Slf4j
@RequiredArgsConstructor
public class LimitExceededConsumer {

    private final NotificationPersistenceService notificationPersistenceService;

    @KafkaListener(topics = "limit.calculate-stats", groupId = "notification-limit-exceeded")
    public void handle(LimitStatsEvent event) {
        if (event.percentage().compareTo(BigDecimal.valueOf(100)) >= 0) {
            notificationPersistenceService.save(event.userId(), new LimitExceededDto(
                    NotificationType.LIMIT_EXCEEDED,
                    LocalDateTime.now(),
                    event.periodType(),
                    event.limitId(),
                    event.percentage()
            ));
            log.info("Limit exceeded for userId={}, limitId={}, periodType={}, usage={}%", event.userId(), event.limitId(), event.periodType(), event.percentage());
        }
    }
}