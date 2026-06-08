package com.finovara.corebackend.notification.service.limit;

import com.finovara.contracts.event.notification.limit.LimitStatsEvent;
import com.finovara.contracts.model.NotificationType;
import com.finovara.corebackend.notification.dto.limit.LimitExceededDto;
import com.finovara.corebackend.notification.service.NotificationPersistenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class LimitExceededService {

    private final NotificationPersistenceService notificationPersistenceService;

    @KafkaListener(topics = "limit.calculate-stats")
    public void handle(LimitStatsEvent event) {
        if (event.percentage().compareTo(BigDecimal.valueOf(100)) >= 0) {
            notificationPersistenceService.save(event.userId(), new LimitExceededDto(
                    NotificationType.LIMIT_EXCEEDED,
                    LocalDateTime.now(),
                    event.periodType(),
                    event.limitId(),
                    event.percentage()
            ));
        }
    }
}