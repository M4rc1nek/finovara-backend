package com.finovara.activitylogservice.activitylog.securitymonitoring.dto;

import com.finovara.contracts.securitymonitoring.model.RiskAction;
import com.finovara.contracts.securitymonitoring.model.RiskRule;
import com.finovara.contracts.securitymonitoring.model.RiskTriggerType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record RiskOperationActivityDto(
        String sourceEventId,
        RiskTriggerType triggerType,
        int score,
        RiskAction action,
        List<RiskRule> riskRuleList,
        LocalDateTime createdAt,
        LocalDate operationDate
) {
}
