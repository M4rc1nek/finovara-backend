package com.finovara.corebackend.notification.service.piggybank;

import com.finovara.contracts.event.notification.piggybank.PiggyBankProgressEvent;
import com.finovara.contracts.model.NotificationType;
import com.finovara.corebackend.notification.dto.piggybank.PiggyBankWarningDto;
import com.finovara.corebackend.notification.service.NotificationPersistenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PiggyBankWarningService {

    private final NotificationPersistenceService notificationPersistenceService;

    @KafkaListener(topics = "piggybank.calculate-progress")
    public void handle(PiggyBankProgressEvent event) {
        boolean isWarning = event.percentage().compareTo(BigDecimal.valueOf(75)) >= 0;
        boolean isBelowLimit = event.percentage().compareTo(BigDecimal.valueOf(100)) < 0;

        if (isWarning && isBelowLimit) {
            notificationPersistenceService.save(event.userId(), new PiggyBankWarningDto(
                    NotificationType.PIGGY_BANK_GOAL_APPROACHING,
                    LocalDateTime.now(),
                    event.goalType(),
                    event.piggyBankName(),
                    event.piggyBankId(),
                    BigDecimal.valueOf(75)
            ));
        }
    }
}