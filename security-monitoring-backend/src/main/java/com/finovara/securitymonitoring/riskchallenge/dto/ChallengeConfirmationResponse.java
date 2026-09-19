package com.finovara.securitymonitoring.riskengine.dto;

public record ChallengeConfirmationResponse(
        Long riskOperationId,
        boolean fullyVerified
) {}