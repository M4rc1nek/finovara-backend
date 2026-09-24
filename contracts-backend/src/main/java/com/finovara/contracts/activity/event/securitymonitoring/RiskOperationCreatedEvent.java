package com.finovara.contracts.activity.event.securitymonitoring;

import com.finovara.contracts.securitymonitoring.model.RiskAction;
import com.finovara.contracts.securitymonitoring.model.RiskRule;
import com.finovara.contracts.securitymonitoring.model.RiskTriggerType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record RiskOperationCreatedEvent(
        Long userId,
        String sourceEventId,
        RiskTriggerType triggerType,
        int score,
        RiskAction action,
        LocalDate operationDate,
        LocalDateTime createdAt,
        List<RiskRule> riskRules
) {
}
