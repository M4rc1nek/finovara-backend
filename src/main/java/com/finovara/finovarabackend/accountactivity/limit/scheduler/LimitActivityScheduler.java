package com.finovara.finovarabackend.accountactivity.limit.scheduler;

import com.finovara.finovarabackend.accountactivity.limit.processor.LimitActivityProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LimitActivityScheduler {

    private final LimitActivityProcessor limitActivityProcessor;

    @Scheduled(cron = "${scheduler.user-activity.limit.delete-cron}", zone = "Europe/Warsaw")
    public void deleteLimitActivities() {
        limitActivityProcessor.deleteLimitActivity();
    }

}
