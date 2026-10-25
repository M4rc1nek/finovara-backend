package com.finovara.financeservice.settings.finances.recurring.processor;

import com.finovara.contracts.mainaccount.transaction.event.transaction.RecurringExecutionSkippedEvent;
import com.finovara.contracts.outbox.OutboxService;
import com.finovara.financeservice.settings.finances.recurring.model.RecurringSettings;
import com.finovara.financeservice.settings.finances.recurring.repository.RecurringSettingsRepository;
import com.finovara.financeservice.settings.finances.recurring.service.execution.RecurringExecutionContext;
import com.finovara.financeservice.settings.finances.recurring.service.execution.RecurringExecutionResult;
import com.finovara.financeservice.settings.finances.recurring.service.execution.RecurringExecutionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class RecurringTransactionProcess {
    private static final int MAX_ITERATIONS = 100;
    private static final String SKIPPED_TOPIC = "notification.recurring-transaction-skipped";

    private final RecurringSettingsRepository recurringSettingsRepository;
    private final RecurringExecutionService recurringExecutionService;
    private final OutboxService outboxService;

    @Transactional
    public void processSingle(Long settingsId, LocalDate today) {
        RecurringSettings settings = recurringSettingsRepository.findDueByIdForUpdateSkipLocked(settingsId, today).orElse(null);
        if (settings == null || !isProcessable(settings)) {
            return;
        }

        RecurringExecutionContext context = recurringExecutionService.prepareContext(settings);

        int iterations = 0;
        int skippedCount = 0;
        LocalDate lastSkippedDate = null;

        LocalDate nextDate = settings.getNextExecutionDate();
        LocalDate endDate = settings.getEndDate();

        while (settings.isEnable() && !nextDate.isAfter(today) && isWithinEndDate(nextDate, endDate) && iterations++ < MAX_ITERATIONS) {
            RecurringExecutionResult result = recurringExecutionService.execute(settings, nextDate, context);

            if (result == RecurringExecutionResult.SKIPPED) {
                skippedCount++;
                lastSkippedDate = nextDate;
            } else if (settings.isEnable()) {
                settings.setSkippedNotificationSent(false);
            }

            if (!settings.isEnable()) {
                break;
            }

            nextDate = settings.getPeriodType().addPeriod(nextDate);
        }

        if (skippedCount > 0) {
            notifySkipped(settings, lastSkippedDate, skippedCount);
        }

        settings.setNextExecutionDate(resolveNextExecutionDate(settings, nextDate, endDate));
    }

    private boolean isProcessable(RecurringSettings settings) {
        return settings.isEnable() && settings.getType() != null && settings.getPeriodType() != null
                && settings.getAmount() != null && settings.getUserId() != null && settings.getNextExecutionDate() != null;
    }

    private boolean isWithinEndDate(LocalDate date, LocalDate endDate) {
        return endDate == null || !date.isAfter(endDate);
    }

    private LocalDate resolveNextExecutionDate(RecurringSettings settings, LocalDate nextDate, LocalDate endDate) {
        if (!settings.isEnable() || !isWithinEndDate(nextDate, endDate)) {
            return null;
        }
        return nextDate;
    }

    private void notifySkipped(RecurringSettings settings, LocalDate lastScheduledDate, int skippedCount) {
        if (settings.isSkippedNotificationSent()) {
            return;
        }

        outboxService.save("RecurringSettings", settings.getId().toString(), SKIPPED_TOPIC,
                new RecurringExecutionSkippedEvent(settings.getUserId(), settings.getType(), settings.getId(),
                        settings.getAmount(), lastScheduledDate, skippedCount, LocalDateTime.now()));

        settings.setSkippedNotificationSent(true);
    }
}