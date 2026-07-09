package com.finovara.activitylogservice.activitylog.accountactivity.sharedaccount.scheduler;

import com.finovara.activitylogservice.activitylog.accountactivity.sharedaccount.processor.SharedAccountActivityProcessor;
import lombok.RequiredArgsConstructor;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SharedAccountActivityScheduler {

    private final SharedAccountActivityProcessor sharedAccountActivityProcessor;

    @Scheduled(cron = "${scheduler.user-activity.shared-account.delete-cron}", zone = "Europe/Warsaw")
    @SchedulerLock(name = "deleteRevenueActivities", lockAtMostFor = "10m", lockAtLeastFor = "30s")
    public void deleteSharedAccountActivities(){
        sharedAccountActivityProcessor.deleteSharedAccountActivity();
    }

}
