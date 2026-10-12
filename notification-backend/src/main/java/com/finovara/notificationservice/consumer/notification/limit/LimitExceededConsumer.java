package com.finovara.notificationservice.consumer.notification.limit;

import com.finovara.contracts.mainaccount.notification.NotificationType;
import com.finovara.contracts.mainaccount.transaction.event.limit.LimitStatsEvent;
import com.finovara.notificationservice.notification.NotificationPersistenceService;
import com.finovara.notificationservice.notification.dto.limit.LimitExceededDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Component
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