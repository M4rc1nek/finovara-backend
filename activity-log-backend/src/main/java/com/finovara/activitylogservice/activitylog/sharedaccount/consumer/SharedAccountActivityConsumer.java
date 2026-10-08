package com.finovara.activitylogservice.activitylog.sharedaccount.consumer;

import com.finovara.activitylogservice.activitylog.sharedaccount.service.SharedAccountActivityLogService;
import com.finovara.contracts.sharedaccount.event.activity.SharedAccountActivityLogEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SharedAccountActivityConsumer {

    private final SharedAccountActivityDataDeleter sharedAccountActivityDataDeleter;
    private final SharedAccountActivityLogService sharedAccountActivityLogService;

    @KafkaListener(topics = "shared-account.activity")
    public void consumeSharedAccountActivityLog(SharedAccountActivityLogEvent event) {
        sharedAccountActivityLogService.createSharedAccountActivityLog(event);
    }

    @KafkaListener(topics = "shared-account.deleted")
    public void deleteDataFromSharedAccount() {
        sharedAccountActivityDataDeleter.deleteAllSharedAccountActivityLogData();
    }
}
