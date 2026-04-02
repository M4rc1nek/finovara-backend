package com.finovara.finovarabackend.accountactivity.limit.scheduler;

import com.finovara.finovarabackend.accountactivity.limit.processor.LimitActivityProcessor;
import lombok.RequiredArgsConstructor;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LimitActivityScheduler {

    private final LimitActivityProcessor limitActivityProcessor;

    @Scheduled(cron = "${scheduler.user-activity.limit.delete-cron}", zone = "Europe/Warsaw")
    @SchedulerLock(name = "deleteLimitActivities", lockAtMostFor = "10m", lockAtLeastFor = "30s")
    public void deleteLimitActivities() {
        limitActivityProcessor.deleteLimitActivity();
    }

}
