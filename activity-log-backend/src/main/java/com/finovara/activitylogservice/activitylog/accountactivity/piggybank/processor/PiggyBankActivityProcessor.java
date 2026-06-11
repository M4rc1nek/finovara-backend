package com.finovara.activityservice.activitylog.accountactivity.piggybank.processor;

import com.finovara.activityservice.activitylog.accountactivity.piggybank.repository.PiggyBankActivityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PiggyBankActivityProcessor {

    private final PiggyBankActivityRepository piggyBankActivityRepository;

    public void deletePiggyBankActivities(){
        piggyBankActivityRepository.deleteAllInBatch();
    }

}
