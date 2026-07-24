package com.finovara.financeservice.sharedaccount.note.scheduler;

import com.finovara.financeservice.sharedaccount.note.processor.SharedAccountNoteProcessor;
import lombok.RequiredArgsConstructor;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SharedAccountNoteScheduler {

    private final SharedAccountNoteProcessor sharedAccountNoteProcessor;

    @Scheduled(cron = "${scheduler.shared-account.notes.delete-cron}", zone = "Europe/Warsaw")
    @SchedulerLock(name = "deleteSharedAccountNotes", lockAtMostFor = "10m", lockAtLeastFor = "30s")
    public void deleteSharedAccountActivities() {
        sharedAccountNoteProcessor.deleteNotes();
    }

}
