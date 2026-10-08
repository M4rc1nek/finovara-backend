package com.finovara.activitylogservice.activitylog.sharedaccount.scheduler;

import com.finovara.activitylogservice.activitylog.sharedaccount.processor.SharedAccountActivityLogProcessor;
import lombok.RequiredArgsConstructor;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SharedAccountActivityLogScheduler {

    private final SharedAccountActivityLogProcessor sharedAccountActivityLogProcessor;

    @Scheduled(cron = "${scheduler.shared-account.activity.delete-cron}", zone = "Europe/Warsaw")
    @SchedulerLock(name = "deleteSharedAccountActivityLogs", lockAtMostFor = "10m", lockAtLeastFor = "30s")
    public void deleteSharedAccountActivityLogs(){
        sharedAccountActivityLogProcessor.deleteSharedAccountActivityLog();
    }

}
