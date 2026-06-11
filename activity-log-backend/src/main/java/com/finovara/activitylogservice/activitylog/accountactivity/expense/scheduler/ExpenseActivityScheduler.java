package com.finovara.activitylogservice.activitylog.accountactivity.expense.scheduler;

import com.finovara.activitylogservice.activitylog.accountactivity.expense.processor.ExpenseActivityProcessor;
import lombok.RequiredArgsConstructor;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ExpenseActivityScheduler {

    private final ExpenseActivityProcessor expenseActivityProcessor;

    @Scheduled(cron = "${scheduler.user-activity.expense.delete-cron}", zone = "Europe/Warsaw")
    @SchedulerLock(name = "deleteExpenseActivity", lockAtMostFor = "10m", lockAtLeastFor = "30s")
    public void deleteExpenseActivity() {
        expenseActivityProcessor.deleteExpenseActivity();
    }
}
