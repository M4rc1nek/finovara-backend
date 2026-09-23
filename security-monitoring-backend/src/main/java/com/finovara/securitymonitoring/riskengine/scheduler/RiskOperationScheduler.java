package com.finovara.securitymonitoring.riskengine.scheduler;

import com.finovara.securitymonitoring.riskengine.processor.RiskOperationProcessor;
import lombok.RequiredArgsConstructor;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RiskOperationScheduler {
    private  final RiskOperationProcessor riskOperationProcessor;

    @Scheduled(cron = "${scheduler.risk-operation.delete-cron}", zone = "Europe/Warsaw")
    @SchedulerLock(name = "deleteRiskOperation", lockAtMostFor = "10m", lockAtLeastFor = "30s")
    public void deleteAllRiskOperationData() {
        riskOperationProcessor.deleteAllRiskOperationData();
    }
}
