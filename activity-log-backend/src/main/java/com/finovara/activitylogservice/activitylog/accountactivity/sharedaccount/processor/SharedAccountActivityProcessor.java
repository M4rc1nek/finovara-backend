package com.finovara.activitylogservice.activitylog.accountactivity.sharedaccount.processor;

import com.finovara.activitylogservice.activitylog.accountactivity.sharedaccount.repository.SharedAccountActivityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SharedAccountActivityProcessor {

    private final SharedAccountActivityRepository sharedAccountActivityRepository;

    public void deleteSharedAccountActivity(){
        sharedAccountActivityRepository.deleteAllInBatch();
    }
}
