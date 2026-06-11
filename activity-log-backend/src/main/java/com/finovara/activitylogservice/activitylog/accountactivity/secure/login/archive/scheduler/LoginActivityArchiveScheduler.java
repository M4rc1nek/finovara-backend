package com.finovara.activitylogservice.activitylog.accountactivity.secure.login.archive.scheduler;

import com.finovara.activitylogservice.activitylog.accountactivity.secure.login.archive.processor.LoginActivityArchiveProcessor;
import lombok.RequiredArgsConstructor;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LoginActivityArchiveScheduler {

    private final LoginActivityArchiveProcessor loginActivityArchiveProcessor;

    @Scheduled(cron = "${scheduler.user-activity.login.archive-cron}", zone = "Europe/Warsaw")
    @SchedulerLock(name = "deleteLoginActivityArchive", lockAtMostFor = "10m", lockAtLeastFor = "30s")
    public void deleteLoginActivityArchive() {
        loginActivityArchiveProcessor.deleteLoginActivitiesFromArchive();
    }
}