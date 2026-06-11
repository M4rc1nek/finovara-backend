package com.finovara.activityservice.activitylog.accountactivity.limit.processor;

import com.finovara.activityservice.activitylog.accountactivity.limit.repository.LimitActivityRepository;
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
