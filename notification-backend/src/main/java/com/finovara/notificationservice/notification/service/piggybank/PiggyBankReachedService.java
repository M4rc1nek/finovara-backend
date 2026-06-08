package com.finovara.corebackend.notification.service.piggybank;

import com.finovara.contracts.event.notification.piggybank.PiggyBankProgressEvent;
import com.finovara.contracts.model.NotificationType;
import com.finovara.corebackend.notification.dto.piggybank.PiggyBankReachedDto;
import com.finovara.corebackend.notification.service.NotificationPersistenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PiggyBankReachedService {

    private final NotificationPersistenceService notificationPersistenceService;

    @KafkaListener(topics = "piggybank.calculate-progress")
    public void handle(PiggyBankProgressEvent event) {
        if (event.percentage().compareTo(BigDecimal.valueOf(100)) >= 0) {
            notificationPersistenceService.save(event.userId(), new PiggyBankReachedDto(
                    NotificationType.PIGGY_BANK_GOAL_REACHED,
                    LocalDateTime.now(),
                    event.goalType(),
                    event.piggyBankName(),
                    event.piggyBankId(),
                    event.percentage()
            ));
        }
    }
}