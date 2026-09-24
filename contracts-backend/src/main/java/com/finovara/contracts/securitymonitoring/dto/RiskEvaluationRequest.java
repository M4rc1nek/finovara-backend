package com.finovara.contracts.securitymonitoring.dto;


import com.finovara.contracts.securitymonitoring.model.RiskTriggerType;

import java.math.BigDecimal;

public record RiskEvaluationRequest(
        Long userId,
        RiskTriggerType triggerType,
        String sourceEventId,
        BigDecimal amount,
        String category,
        String ipAddress,
        String location,
        String browser,
        String email
) {}