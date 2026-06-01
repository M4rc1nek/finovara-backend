package com.finovara.activityservice.kafka;

import com.finovara.activityservice.activitylog.accountactivity.limit.service.LimitActivityService;
import com.finovara.contracts.event.activity.limit.LimitActivityEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LimitActivityConsumer {

    private final LimitActivityService limitActivityService;

    @KafkaListener(topics = "activity.limit", groupId = "activity-service")
    public void handle(LimitActivityEvent event) {
        limitActivityService.handleEvent(event);
    }
}
