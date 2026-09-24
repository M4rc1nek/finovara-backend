package com.finovara.activitylogservice.activitylog.securitymonitoring.scheduler;

import com.finovara.activitylogservice.activitylog.securitymonitoring.processor.RiskOperationActivityProcessor;
import lombok.RequiredArgsConstructor;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RiskOperationActivityScheduler {

    private final RiskOperationActivityProcessor riskOperationActivityProcessor;

    @Scheduled(cron = "${scheduler.user-activity.risk-operation.delete-cron}", zone = "Europe/Warsaw")
    @SchedulerLock(name = "deleteRiskOperationActivity", lockAtMostFor = "10m", lockAtLeastFor = "30s")
    public void deleteRiskOperationActivity() {
        riskOperationActivityProcessor.deleteRiskOperationActivity();
    }
}

