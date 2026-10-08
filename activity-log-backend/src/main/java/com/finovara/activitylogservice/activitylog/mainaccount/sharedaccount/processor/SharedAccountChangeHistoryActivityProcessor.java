package com.finovara.activitylogservice.activitylog.mainaccount.sharedaccount.processor;

import com.finovara.activitylogservice.activitylog.mainaccount.sharedaccount.repository.SharedAccountChangeHistoryActivityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Slf4j
@RequiredArgsConstructor
public class SharedAccountChangeHistoryActivityProcessor {

    private final SharedAccountChangeHistoryActivityRepository sharedAccountChangeHistoryActivityRepository;

    @Transactional
    public void deleteSharedAccountActivity() {
        sharedAccountChangeHistoryActivityRepository.deleteAllInBatch();
        log.info("Shared account change history activity has been deleted.");
    }
}
