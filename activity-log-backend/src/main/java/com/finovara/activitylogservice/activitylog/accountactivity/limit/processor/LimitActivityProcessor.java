package com.finovara.activitylogservice.activitylog.accountactivity.limit.processor;

import com.finovara.activitylogservice.activitylog.accountactivity.limit.repository.LimitActivityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Slf4j
@RequiredArgsConstructor
public class LimitActivityProcessor {

    private final LimitActivityRepository limitActivityRepository;

    @Transactional
    public void deleteLimitActivity(){
        limitActivityRepository.deleteAllInBatch();
        log.info("Limit Activity has been deleted.");
    }

}
