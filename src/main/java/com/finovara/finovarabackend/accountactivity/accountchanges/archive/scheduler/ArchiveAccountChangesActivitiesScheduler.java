package com.finovara.finovarabackend.accountactivity.accountchanges.archive.scheduler;

import com.finovara.finovarabackend.accountactivity.accountchanges.archive.processor.ArchiveAccountChangesActivitiesProcessor;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ArchiveAccountChangesActivitiesScheduler {
    private  final ArchiveAccountChangesActivitiesProcessor archiveAccountChangesActivitiesProcessor;

    @Scheduled(cron = "${scheduler.user-activity.account-changes.archive-cron}", zone = "Europe/Warsaw")
    public void deleteArchiveAccountChanges(){
        archiveAccountChangesActivitiesProcessor.deleteAccountChangesActivities();
    }
}
