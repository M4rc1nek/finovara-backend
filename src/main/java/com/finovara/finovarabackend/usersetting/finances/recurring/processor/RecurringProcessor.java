package com.finovara.finovarabackend.usersetting.finances.recurring.processor;

import com.finovara.finovarabackend.usersetting.finances.recurring.model.RecurringSettings;
import com.finovara.finovarabackend.usersetting.finances.recurring.repository.RecurringSettingsRepository;
import com.finovara.finovarabackend.usersetting.finances.recurring.service.execution.RecurringExecutionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class RecurringProcessor {

    private final RecurringSettingsRepository recurringSettingsRepository;
    private final RecurringExecutionService recurringExecutionService;

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
        int maxIterations = 100;

        LocalDate nextDate = settings.getNextExecutionDate();

        while (!nextDate.isAfter(today) && safetyCounter++ < maxIterations) {
            recurringExecutionService.execute(settings, nextDate);
            nextDate = settings.getPeriodType().addPeriod(nextDate);
        }

        settings.setNextExecutionDate(nextDate);
        recurringSettingsRepository.save(settings);
    }

}