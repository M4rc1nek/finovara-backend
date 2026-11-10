package com.finovara.financeservice.trash.scheduler;

import com.finovara.financeservice.trash.processor.TrashProcessor;
import lombok.RequiredArgsConstructor;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TrashScheduler {

    private final TrashProcessor trashProcessor;

    @Scheduled(cron = "${scheduler.delete-main-account-trash}", zone = "Europe/Warsaw")
    @SchedulerLock(name = "deleteMainAccountTrash", lockAtMostFor = "10m", lockAtLeastFor = "1m")
    public void deleteTrash() {
        trashProcessor.deleteAllTrashes();
    }
}
