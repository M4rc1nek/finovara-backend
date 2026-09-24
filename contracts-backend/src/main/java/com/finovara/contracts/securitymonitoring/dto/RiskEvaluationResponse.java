package com.finovara.contracts.securitymonitoring.dto;

import com.finovara.contracts.securitymonitoring.model.RiskAction;

public record RiskEvaluationResponse(
        Long riskOperationId,
        int score,
        RiskAction action,
        boolean passwordRequired,
        boolean emailCodeRequired,
        boolean isFullyVerified
) {}