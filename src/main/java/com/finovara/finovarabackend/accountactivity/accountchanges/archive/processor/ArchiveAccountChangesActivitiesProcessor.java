package com.finovara.finovarabackend.accountactivity.accountchanges.archive.processor;

import com.finovara.finovarabackend.accountactivity.accountchanges.archive.repository.ArchiveAccountChangesActivitiesRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class ArchiveAccountChangesActivitiesProcessor {

    private final ArchiveAccountChangesActivitiesRepository archiveAccountChangesActivitiesRepository;

    @Transactional
    public void deleteAccountChangesActivities() {
        archiveAccountChangesActivitiesRepository.deleteAllInBatch();
        log.info("Account change activities were deleted.");
    }
}
