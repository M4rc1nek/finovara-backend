package com.finovara.activityservice.activitylog.accountactivity.secure.accountchange.archive.scheduler;

import com.finovara.activityservice.activitylog.accountactivity.secure.accountchange.archive.processor.AccountChangeArchiveProcessor;
import lombok.RequiredArgsConstructor;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AccountChangeArchiveScheduler {
    private  final AccountChangeArchiveProcessor accountChangeArchiveProcessor;

    @Scheduled(cron = "${scheduler.user-activity.account-changes.archive-cron}", zone = "Europe/Warsaw")
    @SchedulerLock(name = "deleteAccountChangeArchive", lockAtMostFor = "10m", lockAtLeastFor = "30s")
    public void deleteAccountChangeArchive(){
        accountChangeArchiveProcessor.deleteAccountChangeActivities();
    }
}
