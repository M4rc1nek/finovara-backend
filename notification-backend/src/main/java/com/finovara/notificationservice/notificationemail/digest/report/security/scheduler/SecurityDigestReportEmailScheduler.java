package com.finovara.notificationservice.notificationemail.service.digest.report.security.scheduler;

import com.finovara.notificationservice.notificationemail.service.digest.report.security.processor.WeeklySecurityDigestReportEmailProcessor;
import lombok.RequiredArgsConstructor;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SecurityDigestReportEmailScheduler {

    private final WeeklySecurityDigestReportEmailProcessor weeklySecurityDigestReportEmailProcessor;

    @Scheduled(cron = "${scheduler.email-notification.weekly-security-digest-report-send}", zone = "Europe/Warsaw")
    @SchedulerLock(name = "sendWeeklySecurityDigestEmail", lockAtMostFor = "10m", lockAtLeastFor = "30s")
    public void sendWeeklySecurityDigestEmail() {
        weeklySecurityDigestReportEmailProcessor.sendWeeklySecurityDigestEmail();
    }

}
