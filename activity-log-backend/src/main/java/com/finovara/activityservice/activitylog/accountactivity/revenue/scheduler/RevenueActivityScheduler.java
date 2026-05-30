package com.finovara.activityservice.activitylog.accountactivity.revenue.scheduler;

import com.finovara.activityservice.activitylog.accountactivity.revenue.processor.RevenueActivityProcessor;
import lombok.RequiredArgsConstructor;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RevenueActivityScheduler {

    private final RevenueActivityProcessor revenueActivityProcessor;

    @Scheduled(cron = "${scheduler.user-activity.revenue.delete-cron}", zone = "Europe/Warsaw")
    @SchedulerLock(name = "deleteRevenueActivities", lockAtMostFor = "10m", lockAtLeastFor = "30s")
    public void deleteRevenueActivities(){
        revenueActivityProcessor.deleteRevenueActivity();
    }

}
