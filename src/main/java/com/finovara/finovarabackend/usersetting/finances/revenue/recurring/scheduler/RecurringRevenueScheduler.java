package com.finovara.finovarabackend.usersetting.finances.revenue.recurring.scheduler;

import com.finovara.finovarabackend.usersetting.finances.revenue.recurring.processor.RecurringRevenueProcessor;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RecurringRevenueScheduler {
    private final RecurringRevenueProcessor recurringRevenueProcessor;


    @Scheduled(cron = "${scheduler.revenue-settings.recurring-frequency}", zone = "Europe/Warsaw")
    public void getRecurringRevenue(){
        recurringRevenueProcessor.generateRecurringRevenues();
    }
}
