package com.finovara.finovarabackend.accountactivity.expense.scheduler;

import com.finovara.finovarabackend.accountactivity.expense.processor.ExpenseActivityProcessor;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ExpenseActivityScheduler {

    private final ExpenseActivityProcessor expenseActivityProcessor;

    @Scheduled(cron = "${scheduler.user-activity.expense.delete-cron}", zone = "Europe/Warsaw")
    public void deleteExpenseActivity() {
        expenseActivityProcessor.deleteExpenseActivity();
    }
}
