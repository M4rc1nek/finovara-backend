package com.finovara.activityservice.kafka;

import com.finovara.activityservice.activity_log.accountactivity.piggybank.service.PiggyBankActivityService;
import com.finovara.contracts.event.piggybank.PiggyBankActivityEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PiggyBankActivityConsumer {

    private final PiggyBankActivityService piggyBankActivityService;

    @KafkaListener(topics = "activity.piggybank", groupId = "activity-service")
    public void handle(PiggyBankActivityEvent event) {
        piggyBankActivityService.handleEvent(event);
    }
}
