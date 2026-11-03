package com.finovara.financeservice.revenue.scheduler;

import com.finovara.financeservice.revenue.processor.RevenueProcessor;
import lombok.RequiredArgsConstructor;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RevenueScheduler {

    private final RevenueProcessor revenueProcessor;

    @Scheduled(cron = "${scheduler.delete-all-revenues}", zone = "Europe/Warsaw")
    @SchedulerLock(name = "deleteAllRevenues", lockAtMostFor = "10m", lockAtLeastFor = "30s")
    public void deleteAllRevenues() {
        revenueProcessor.deleteAllRevenues();
    }
}
