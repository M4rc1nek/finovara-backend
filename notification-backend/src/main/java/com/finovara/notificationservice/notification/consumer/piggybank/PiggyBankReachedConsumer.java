package com.finovara.notificationservice.notification.consumer.piggybank;

import com.finovara.contracts.event.notification.piggybank.PiggyBankProgressEvent;
import com.finovara.contracts.model.NotificationType;
import com.finovara.notificationservice.notification.dto.piggybank.PiggyBankReachedDto;
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
public class PiggyBankReachedConsumer {

    private final NotificationPersistenceService notificationPersistenceService;

    @KafkaListener(topics = "piggybank.calculate-progress", groupId = "notification-piggybank-reached")
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
            log.info("Piggy bank goal reached for userId={}, piggyBankId={}, piggyBankName='{}', goalType={}, progress={}%", event.userId(), event.piggyBankId(), event.piggyBankName(), event.goalType(), event.percentage());
        }
    }
}