package com.finovara.securitymonitoring.riskengine.dto;

import com.finovara.securitymonitoring.riskengine.model.RiskAction;

public record RiskEvaluationResponse(
        Long riskOperationId,
        int score,
        RiskAction action,
        boolean passwordRequired,
        boolean emailCodeRequired
) {}