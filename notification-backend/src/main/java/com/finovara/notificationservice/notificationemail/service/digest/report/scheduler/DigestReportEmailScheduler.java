package com.finovara.notificationservice.notificationemail.service.digest.report.scheduler;

import com.finovara.notificationservice.notificationemail.service.digest.report.processor.WeeklyDigestReportEmailProcessor;
import lombok.RequiredArgsConstructor;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DigestReportEmailScheduler {

    private final WeeklyDigestReportEmailProcessor weeklyDigestReportEmailProcessor;

    @Scheduled(cron = "${scheduler.email-notification.weekly-digest-report-send}", zone = "Europe/Warsaw")
    @SchedulerLock(name = "sendDigestEmailReport", lockAtMostFor = "10m", lockAtLeastFor = "30s")
    public void sendDigestReportEmail(){
        weeklyDigestReportEmailProcessor.sendDigestEmailReport();
    }

}
