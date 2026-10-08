package com.finovara.activitylogservice.activitylog.mainaccount.sharedaccount.scheduler;

import com.finovara.activitylogservice.activitylog.mainaccount.sharedaccount.processor.SharedAccountChangeHistoryActivityProcessor;
import lombok.RequiredArgsConstructor;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SharedAccountChangeHistoryActivityScheduler {

    private final SharedAccountChangeHistoryActivityProcessor sharedAccountChangeHistoryActivityProcessor;

    @Scheduled(cron = "${scheduler.user-activity.shared-account-change-history.delete-cron}", zone = "Europe/Warsaw")
    @SchedulerLock(name = "deleteRevenueActivities", lockAtMostFor = "10m", lockAtLeastFor = "30s")
    public void deleteSharedAccountActivities(){
        sharedAccountChangeHistoryActivityProcessor.deleteSharedAccountActivity();
    }

}
