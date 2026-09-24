package com.finovara.activitylogservice.activitylog.securitymonitoring.consumer;

import com.finovara.activitylogservice.activitylog.securitymonitoring.service.RiskOperationLogService;
import com.finovara.contracts.activity.event.securitymonitoring.RiskOperationCreatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RiskOperationConsumer {

    private final RiskOperationLogService riskOperationLogService;

    @KafkaListener(topics = "risk-operation.created")
    public void handleSettings(RiskOperationCreatedEvent event) {
        riskOperationLogService.handleEvent(event);
    }

}
