package com.finovara.activitylogservice.activitylog.accountactivity.piggybank.scheduler;

import com.finovara.activitylogservice.activitylog.accountactivity.piggybank.processor.PiggyBankActivityProcessor;
import lombok.RequiredArgsConstructor;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PiggyBankActivityScheduler {

    private final PiggyBankActivityProcessor piggyBankActivityProcessor;

    @Scheduled(cron = "${scheduler.user-activity.piggy-bank.delete-cron}", zone = "Europe/Warsaw")
    @SchedulerLock(name = "deletePiggyBankActivities", lockAtMostFor = "10m", lockAtLeastFor = "30s")
    public void deletePiggyBankActivities() {
        piggyBankActivityProcessor.deletePiggyBankActivities();
    }

}
