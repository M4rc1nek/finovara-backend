package com.finovara.activitylogservice.activitylog.sharedaccount.processor;

import com.finovara.activitylogservice.activitylog.sharedaccount.repository.SharedAccountActivityLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class SharedAccountActivityLogProcessor {

    private final SharedAccountActivityLogRepository sharedAccountActivityLogRepository;

    @Transactional
    public void deleteSharedAccountActivityLog() {
        sharedAccountActivityLogRepository.deleteAllInBatch();
        log.info("Shared account activity log has been deleted.");
    }
}
