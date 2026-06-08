package com.finovara.corebackend.notification.service.limit;

import com.finovara.contracts.event.notification.limit.LimitStatsEvent;
import com.finovara.contracts.model.NotificationType;
import com.finovara.corebackend.notification.dto.limit.LimitWarningDto;
import com.finovara.corebackend.notification.service.NotificationPersistenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class LimitWarningService {

    private final NotificationPersistenceService notificationPersistenceService;

    @KafkaListener(topics = "llimit.calculate-stats")
    public void handle(LimitStatsEvent event) {
        boolean isWarning = event.percentage().compareTo(BigDecimal.valueOf(75)) >= 0;
        boolean isBelowLimit = event.percentage().compareTo(BigDecimal.valueOf(100)) < 0;

        if (isWarning && isBelowLimit) {
            notificationPersistenceService.save(event.userId(), new LimitWarningDto(
                    NotificationType.LIMIT_EXCEEDED_WARNING,
                    LocalDateTime.now(),
                    event.percentage(),
                    event.periodType(),
                    event.limitId(),
                    BigDecimal.valueOf(75)
            ));
        }
    }
}