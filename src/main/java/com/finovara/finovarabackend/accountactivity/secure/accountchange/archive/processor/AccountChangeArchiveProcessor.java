package com.finovara.finovarabackend.accountactivity.secure.accountchange.archive.processor;

import com.finovara.finovarabackend.accountactivity.secure.accountchange.archive.repository.AccountChangeArchiveRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class AccountChangeArchiveProcessor {

    private final AccountChangeArchiveRepository accountChangeArchiveRepository;

    @Transactional
    public void deleteAccountChangeActivities() {
        accountChangeArchiveRepository.deleteAllInBatch();
        log.info("Account change activities were deleted.");
    }
}
