package com.finovara.financeservice.sharedaccount.revenue.scheduler;

import com.finovara.financeservice.sharedaccount.revenue.processor.SharedRevenueProcessor;
import lombok.RequiredArgsConstructor;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SharedRevenueScheduler {

    private final SharedRevenueProcessor sharedRevenueProcessor;

    @Scheduled(cron = "${scheduler.delete-all-shared-revenues}", zone = "Europe/Warsaw")
    @SchedulerLock(name = "deleteAllSharedRevenues", lockAtMostFor = "10m", lockAtLeastFor = "30s")
    public void deleteAllSharedRevenues() {
        sharedRevenueProcessor.deleteAllSharedRevenues();
    }
}
