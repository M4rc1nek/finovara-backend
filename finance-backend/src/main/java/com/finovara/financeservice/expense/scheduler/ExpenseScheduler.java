package com.finovara.financeservice.expense.scheduler;

import com.finovara.financeservice.expense.processor.ExpenseProcessor;
import lombok.RequiredArgsConstructor;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ExpenseScheduler {

    private final ExpenseProcessor expenseProcessor;

    @Scheduled(cron = "${scheduler.delete-all-expenses}", zone = "Europe/Warsaw")
    @SchedulerLock(name = "deleteAllExpenses", lockAtMostFor = "10m", lockAtLeastFor = "30s")
    public void deleteAllExpenses() {
        expenseProcessor.deleteAllExpenses();
    }
}
