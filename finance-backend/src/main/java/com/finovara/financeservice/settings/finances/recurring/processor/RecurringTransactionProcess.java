package com.finovara.financeservice.settings.finances.recurring.processor;

import com.finovara.contracts.notification.event.recurring.transaction.RecurringExecutionSkippedEvent;
import com.finovara.financeservice.settings.finances.recurring.model.RecurringSettings;
import com.finovara.financeservice.settings.finances.recurring.repository.RecurringSettingsRepository;
import com.finovara.financeservice.settings.finances.recurring.service.execution.RecurringExecutionResult;
import com.finovara.financeservice.settings.finances.recurring.service.execution.RecurringExecutionService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class RecurringTransactionProcess {
    private static final int MAX_ITERATIONS = 100;

    private final RecurringSettingsRepository recurringSettingsRepository;
    private final RecurringExecutionService recurringExecutionService;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processSingle(RecurringSettings settings, LocalDate today) {
        int safetyCounter = 0;
        int skippedCount = 0;
        LocalDate lastSkippedDate = null;

        LocalDate nextDate = settings.getNextExecutionDate();
        LocalDate endDate = settings.getEndDate();

        while (settings.isEnable() && !nextDate.isAfter(today) && (endDate == null || !nextDate.isAfter(endDate)) && safetyCounter++ < MAX_ITERATIONS) {
            RecurringExecutionResult result = recurringExecutionService.execute(settings, nextDate);

            if (result == RecurringExecutionResult.SKIPPED) {
                skippedCount++;
                lastSkippedDate = nextDate;
            }

            if (!settings.isEnable()) {
                break;
            }

            nextDate = settings.getPeriodType().addPeriod(nextDate);
        }

        if (skippedCount > 0) {
            notifySkipped(settings, lastSkippedDate, skippedCount);
        }

        settings.setNextExecutionDate(settings.isEnable() ? nextDate : null);
        recurringSettingsRepository.save(settings);
    }

    private void notifySkipped(RecurringSettings settings, LocalDate lastScheduledDate, int skippedCount) {
        if (skippedCount <= 0 || settings.isSkippedNotificationSent()) {
            return;
        }

        kafkaTemplate.send("activity.recurring.skipped",
                new RecurringExecutionSkippedEvent(settings.getUserId(), settings.getType(), settings.getId(),
                        settings.getAmount(), lastScheduledDate, skippedCount, LocalDateTime.now()));

        settings.setSkippedNotificationSent(true);
    }
}