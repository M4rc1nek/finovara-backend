package com.finovara.activitylogservice.activitylog.accountactivity.piggybank.processor;

import com.finovara.activitylogservice.activitylog.accountactivity.piggybank.repository.PiggyBankActivityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Slf4j
@RequiredArgsConstructor
public class PiggyBankActivityProcessor {

    private final PiggyBankActivityRepository piggyBankActivityRepository;

    @Transactional
    public void deletePiggyBankActivities(){
        piggyBankActivityRepository.deleteAllInBatch();
        log.info("Piggy bank activities has been deleted.");
    }

}
