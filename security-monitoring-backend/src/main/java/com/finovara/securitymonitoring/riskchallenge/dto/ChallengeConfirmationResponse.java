package com.finovara.securitymonitoring.riskchallenge.dto;

public record ChallengeConfirmationResponse(
        Long riskOperationId,
        boolean fullyVerified
) {}