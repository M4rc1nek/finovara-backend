package com.finovara.financeservice.sharedaccount.expense.scheduler;

import com.finovara.financeservice.sharedaccount.expense.processor.SharedExpenseProcessor;
import lombok.RequiredArgsConstructor;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SharedExpenseScheduler {

    private final SharedExpenseProcessor sharedExpenseProcessor;

    @Scheduled(cron = "${scheduler.delete-all-shared-expenses}", zone = "Europe/Warsaw")
    @SchedulerLock(name = "deleteAllSharedExpenses", lockAtMostFor = "10m", lockAtLeastFor = "30s")
    public void deleteAllSharedExpenses() {
        sharedExpenseProcessor.deleteAllSharedExpenses();
    }
}
