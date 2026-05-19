package com.finovara.finovarabackend.accountactivity.limit.processor;

import com.finovara.finovarabackend.accountactivity.limit.repository.LimitActivityRepository;
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
