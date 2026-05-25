package com.finovara.activityservice.activity_log.accountactivity.limit.processor;

import com.finovara.activityservice.activity_log.accountactivity.limit.repository.LimitActivityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LimitActivityProcessor {

    private final LimitActivityRepository limitActivityRepository;

    public void deleteLimitActivity(){
        limitActivityRepository.deleteAllInBatch();
    }

}
