package com.finovara.financeservice.settings.finances.recurring.processor;

import com.finovara.financeservice.settings.finances.recurring.repository.RecurringSettingsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class RecurringProcessor {

    private final RecurringSettingsRepository recurringSettingsRepository;
    private final RecurringTransactionProcess recurringTransactionProcess;

    @Value("${scheduler.recurring-settings.batch-size}")
    private int batchSize;

    public void generateRecurringTransaction() {
        log.info("Started generating recurring transaction");
        LocalDate today = LocalDate.now();

        int handledSettingsCount = 0;
        Long lastProcessedSettingsId = 0L;
        List<Long> dueSettingsIds;

        do {
            dueSettingsIds = recurringSettingsRepository.findDueSettingsIds(today, lastProcessedSettingsId, PageRequest.of(0, batchSize));
            for (Long settingsId : dueSettingsIds) {
                processSafely(settingsId, today);
            }
            handledSettingsCount += dueSettingsIds.size();
            if (!dueSettingsIds.isEmpty()) {
                lastProcessedSettingsId = dueSettingsIds.get(dueSettingsIds.size() - 1);
            }
        } while (dueSettingsIds.size() == batchSize);

        log.info("Finished generating recurring transaction, due settings handled: {}", handledSettingsCount);
    }

    private void processSafely(Long settingsId, LocalDate today) {
        try {
            recurringTransactionProcess.processSingle(settingsId, today);
        } catch (Exception exception) {
            log.error("Failed to process recurring settings id={}", settingsId, exception);
        }
    }
}