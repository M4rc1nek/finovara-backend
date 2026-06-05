package com.finovara.corebackend.usersetting.finances.recurring.processor;

import com.finovara.corebackend.usersetting.finances.recurring.model.RecurringSettings;
import com.finovara.corebackend.usersetting.finances.recurring.repository.RecurringSettingsRepository;
import com.finovara.corebackend.usersetting.finances.recurring.service.execution.RecurringExecutionService;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class RecurringProcessor {
    private static final int MAX_ITERATIONS = 100;

    private final RecurringSettingsRepository recurringSettingsRepository;
    private final RecurringExecutionService recurringExecutionService;

    @Transactional
    public void generateRecurringTransaction() {
        log.info("Started generating recurring transaction");
        LocalDate today = LocalDate.now();
        List<RecurringSettings> settingsList = recurringSettingsRepository.findDueRecurring(today);

        for (RecurringSettings settings : settingsList) {
            if (!settings.isEnable() || settings.getNextExecutionDate() == null || settings.getPeriodType() == null ||
                    settings.getAmount() == null || settings.getUserAssigned() == null) {
                continue;
            }

            processSingle(settings, today);
        }
    }

    private void processSingle(RecurringSettings settings, LocalDate today) {
        int safetyCounter = 0;

        LocalDate nextDate = settings.getNextExecutionDate();

        while (settings.isEnable() && !nextDate.isAfter(today) && safetyCounter++ < MAX_ITERATIONS) {
            recurringExecutionService.execute(settings, nextDate);

            if (!settings.isEnable()) {
                break;
            }

            nextDate = settings.getPeriodType().addPeriod(nextDate);
        }

        settings.setNextExecutionDate(settings.isEnable() ? nextDate : null);
        recurringSettingsRepository.save(settings);
    }

}
