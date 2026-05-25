package com.finovara.corebackend.usersetting.finances.recurring.scheduler;

import com.finovara.corebackend.usersetting.finances.recurring.processor.RecurringProcessor;
import lombok.RequiredArgsConstructor;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RecurringTransactionScheduler {
    private final RecurringProcessor recurringProcessor;

    @Scheduled(cron = "${scheduler.recurring-settings.frequency}", zone = "Europe/Warsaw")
    @SchedulerLock(name = "getRecurringTransaction", lockAtMostFor = "10m", lockAtLeastFor = "30s")
    public void processRecurringTransaction() {
        recurringProcessor.generateRecurringTransaction();
    }
}
