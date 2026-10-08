package com.finovara.activitylogservice.activitylog.sharedaccount.consumer;

import com.finovara.activitylogservice.activitylog.sharedaccount.repository.SharedAccountActivityLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SharedAccountActivityDataDeleter {

    private final SharedAccountActivityLogRepository financeActivityRepository;

    @Transactional
    public void deleteAllSharedAccountActivityLogData() {
        financeActivityRepository.deleteAllInBatch();
        log.info("Shared account finance activities has been deleted.");
    }

}
