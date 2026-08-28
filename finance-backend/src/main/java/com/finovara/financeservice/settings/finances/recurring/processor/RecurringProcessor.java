package com.finovara.financeservice.settings.finances.recurring.processor;

import com.finovara.financeservice.settings.finances.recurring.model.RecurringSettings;
import com.finovara.financeservice.settings.finances.recurring.repository.RecurringSettingsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class RecurringProcessor {

    private final RecurringSettingsRepository recurringSettingsRepository;
    private final RecurringTransactionProcess recurringTransactionProcess;

    @Transactional
    public void generateRecurringTransaction() {
        log.info("Started generating recurring transaction");
        LocalDate today = LocalDate.now();
        List<RecurringSettings> settingsList = recurringSettingsRepository.findDueRecurring(today);

        for (RecurringSettings settings : settingsList) {
            if (!settings.isEnable() || settings.getNextExecutionDate() == null || settings.getPeriodType() == null ||
                    settings.getAmount() == null || settings.getUserId() == null) {
                continue;
            }

            try {
                recurringTransactionProcess.processSingle(settings, today);
            } catch (Exception exception) {
                log.error("Failed to process recurring settings id={} for userId={}", settings.getId(), settings.getUserId(), exception);
            }
        }
    }

}
