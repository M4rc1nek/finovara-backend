package com.finovara.activitylogservice.activitylog.accountactivity.sharedaccount.processor;

import com.finovara.activitylogservice.activitylog.accountactivity.sharedaccount.repository.SharedAccountActivityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Slf4j
@RequiredArgsConstructor
public class SharedAccountActivityProcessor {

    private final SharedAccountActivityRepository sharedAccountActivityRepository;

    @Transactional
    public void deleteSharedAccountActivity() {
        sharedAccountActivityRepository.deleteAllInBatch();
        log.info("Shared account activity has been deleted.");
    }
}
