package com.finovara.finovarabackend.accountactivity.login.archive.scheduler;

import com.finovara.finovarabackend.accountactivity.login.archive.processor.ArchiveLoginActivityProcessor;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ArchiveLoginActivityScheduler {

    private final ArchiveLoginActivityProcessor archiveLoginActivityProcessor;

    @Scheduled(cron = "${scheduler.user-activity.login.archive-cron}")
    public void deleteLoginActivity() {
        archiveLoginActivityProcessor.deleteLoginActivitiesFromArchive();
    }
}