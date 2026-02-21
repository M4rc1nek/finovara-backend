package com.finovara.finovarabackend.accountactivity.login.archive.processor;

import com.finovara.finovarabackend.accountactivity.login.archive.repository.ArchiveLoginActivityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class ArchiveLoginActivityProcessor {

    private final ArchiveLoginActivityRepository archiveLoginActivityRepository;

    @Transactional
    public void deleteLoginActivitiesFromArchive() {
        archiveLoginActivityRepository.deleteAllInBatch();
        log.info("Login Activities were deleted.");
    }

}
