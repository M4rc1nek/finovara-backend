package com.finovara.notificationservice.notificationemail.service.digest.report.finance.scheduler;

import com.finovara.notificationservice.notificationemail.service.digest.report.finance.processor.WeeklyFinanceDigestReportEmailProcessor;
import lombok.RequiredArgsConstructor;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DigestReportEmailScheduler {

    private final WeeklyFinanceDigestReportEmailProcessor weeklyFinanceDigestReportEmailProcessor;

    @Scheduled(cron = "${scheduler.email-notification.weekly-finance-digest-report-send}", zone = "Europe/Warsaw")
    @SchedulerLock(name = "sendWeeklyFinanceDigestEmail", lockAtMostFor = "10m", lockAtLeastFor = "30s")
    public void sendWeeklyFinanceDigestEmail() {
        weeklyFinanceDigestReportEmailProcessor.sendWeeklyFinanceDigestEmail();
    }

}
