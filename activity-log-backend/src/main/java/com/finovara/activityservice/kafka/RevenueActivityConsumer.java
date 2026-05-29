package com.finovara.activityservice.kafka;

import com.finovara.activityservice.activitylog.accountactivity.revenue.service.RevenueActivityService;
import com.finovara.contracts.event.revenue.RevenueActivityEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RevenueActivityConsumer {

    private final RevenueActivityService revenueActivityService;

    @KafkaListener(topics = "activity.revenue", groupId = "activity-service")
    public void handle(RevenueActivityEvent event) {
        revenueActivityService.handleEvent(event);
    }
}
