package com.finovara.activityservice.kafka;

import com.finovara.activityservice.activity_log.accountactivity.secure.accountchange.activity.service.AccountChangesActivityService;
import com.finovara.contracts.event.secure.accountchange.activity.AccountChangesActivityEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AccountChangesActivityConsumer {

    private final AccountChangesActivityService accountChangesActivityService;

    @KafkaListener(topics = "activity.account-changes", groupId = "activity-service")
    public void handle(AccountChangesActivityEvent event) {
        accountChangesActivityService.handleEvent(event);
    }
}
