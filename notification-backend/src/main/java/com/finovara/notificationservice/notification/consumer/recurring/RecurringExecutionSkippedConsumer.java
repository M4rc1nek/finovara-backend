package com.finovara.notificationservice.notification.consumer.recurring;

import com.finovara.contracts.model.NotificationType;
import com.finovara.contracts.notification.event.recurring.transaction.RecurringExecutionSkippedEvent;
import com.finovara.notificationservice.notification.NotificationPersistenceService;
import com.finovara.notificationservice.notification.dto.recurring.RecurringExecutionSkippedDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@Slf4j
@RequiredArgsConstructor
public class RecurringExecutionSkippedConsumer {

    private final NotificationPersistenceService notificationPersistenceService;

    @KafkaListener(topics = "notification.recurring-transaction-skipped", groupId = "notification-recurring-skipped")
    public void handle(RecurringExecutionSkippedEvent event) {
        notificationPersistenceService.save(event.userId(), new RecurringExecutionSkippedDto(
                NotificationType.RECURRING_EXECUTION_SKIPPED,
                LocalDateTime.now(),
                event.type(),
                event.recurringSettingsId(),
                event.amount(),
                event.lastScheduledDate(),
                event.skippedCount()
        ));

        log.info("Recurring execution skipped notification saved: userId={}, recurringSettingsId={}, type={}, lastScheduledDate={}, skippedTransactions={}",
                event.userId(), event.recurringSettingsId(), event.type(), event.lastScheduledDate(), event.skippedCount());
    }
}