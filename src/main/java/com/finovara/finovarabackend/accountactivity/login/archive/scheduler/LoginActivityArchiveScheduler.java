package com.finovara.finovarabackend.accountactivity.login.archive.scheduler;

import com.finovara.finovarabackend.accountactivity.login.archive.processor.LoginActivityArchiveProcessor;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LoginActivityArchiveScheduler {

    private final LoginActivityArchiveProcessor loginActivityArchiveProcessor;

    @Scheduled(cron = "${scheduler.user-activity.login.archive-cron}", zone = "Europe/Warsaw")
    public void deleteLoginActivityArchive() {
        loginActivityArchiveProcessor.deleteLoginActivitiesFromArchive();
    }
}