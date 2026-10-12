package com.finovara.notificationservice.consumer.notification.piggybank;

import com.finovara.contracts.mainaccount.notification.NotificationType;
import com.finovara.contracts.mainaccount.transaction.event.piggybank.PiggyBankProgressEvent;
import com.finovara.notificationservice.notification.NotificationPersistenceService;
import com.finovara.notificationservice.notification.dto.piggybank.PiggyBankWarningDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Component
@Slf4j
@RequiredArgsConstructor
public class PiggyBankWarningConsumer {

    private final NotificationPersistenceService notificationPersistenceService;

    @KafkaListener(topics = "piggybank.calculate-progress", groupId = "notification-piggybank-warning")
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
            log.info("Piggy bank goal approaching: userId={}, piggyBankId={}, piggyBankName='{}', progress={}%, threshold=75%", event.userId(), event.piggyBankId(), event.piggyBankName(), event.percentage());
        }
    }
}