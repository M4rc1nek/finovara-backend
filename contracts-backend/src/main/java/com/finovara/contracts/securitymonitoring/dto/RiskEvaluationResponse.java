package com.finovara.contracts.securitymonitoring.dto;


public record RiskEvaluationResponse(
        Long riskOperationId,
        int score,
        RiskAction action,
        boolean passwordRequired,
        boolean emailCodeRequired
) {}