package com.finovara.finovarabackend.accountactivity.accountchanges.archive.scheduler;

import com.finovara.finovarabackend.accountactivity.accountchanges.archive.processor.AccountChangeArchiveProcessor;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AccountChangeArchiveScheduler {
    private  final AccountChangeArchiveProcessor accountChangeArchiveProcessor;

    @Scheduled(cron = "${scheduler.user-activity.account-changes.archive-cron}", zone = "Europe/Warsaw")
    public void deleteAccountChangeArchive(){
        accountChangeArchiveProcessor.deleteAccountChangeActivities();
    }
}
