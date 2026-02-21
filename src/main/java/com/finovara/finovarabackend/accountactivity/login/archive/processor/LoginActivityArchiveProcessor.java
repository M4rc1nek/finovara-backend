package com.finovara.finovarabackend.accountactivity.login.archive.processor;

import com.finovara.finovarabackend.accountactivity.login.archive.repository.LoginActivityArchiveRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class LoginActivityArchiveProcessor {

    private final LoginActivityArchiveRepository loginActivityArchiveRepository;

    @Transactional
    public void deleteLoginActivitiesFromArchive() {
        loginActivityArchiveRepository.deleteAllInBatch();
        log.info("Login Activities were deleted.");
    }

}
