package com.finovara.finovarabackend.accountactivity.revenue.scheduler;

import com.finovara.finovarabackend.accountactivity.revenue.processor.RevenueActivityProcessor;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RevenueActivityScheduler {

    private final RevenueActivityProcessor revenueActivityProcessor;

    @Scheduled(cron = "${scheduler.user-activity.revenue.delete-cron}", zone = "Europe/Warsaw")
    public void deleteRevenueActivities(){
        revenueActivityProcessor.deleteRevenueActivity();
    }

}
