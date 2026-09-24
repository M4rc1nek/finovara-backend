package com.finovara.contracts.activity.event.securitymonitoring;

import com.finovara.contracts.securitymonitoring.dto.RiskAction;
import com.finovara.contracts.securitymonitoring.dto.RiskRule;
import com.finovara.contracts.securitymonitoring.dto.RiskTriggerType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record RiskOperationCreated(
        String sourceEventId,
        RiskTriggerType triggerType,
        int score,
        RiskAction action,
        LocalDate operationDate,
        LocalDateTime createdAt,
        List<RiskRule> riskRules
) {
}
