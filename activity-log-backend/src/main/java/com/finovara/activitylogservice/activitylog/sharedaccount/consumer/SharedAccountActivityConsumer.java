package com.finovara.activitylogservice.internal.security.sharedaccount.consumer;

import com.finovara.activitylogservice.internal.security.sharedaccount.consumer.factory.SharedAccountActivityFactory;
import com.finovara.contracts.sharedaccount.event.activity.SharedAccountActivityEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SharedAccountActivityConsumer {

    private final SharedAccountActivityFactory sharedAccountActivityFactory;
    private final SharedAccountActivityDataDeleter sharedAccountActivityDataDeleter;

    @KafkaListener(topics = "shared-account.activity")
    public void consumeExpenseCreated(SharedAccountActivityEvent event) {
        sharedAccountActivityFactory.createSharedAccountActivityLog(event);
    }

    @KafkaListener(topics = "shared-account.deleted")
    public void deleteDataFromSharedAccount() {
        sharedAccountActivityDataDeleter.deleteAllFinanceActivityData();
    }
}
