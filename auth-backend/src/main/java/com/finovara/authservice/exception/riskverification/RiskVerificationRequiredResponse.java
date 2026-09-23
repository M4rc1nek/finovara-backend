package com.finovara.authservice.exception.riskverification;

import com.finovara.contracts.securitymonitoring.dto.RiskAction;

public record RiskVerificationRequiredResponse(
        Long riskOperationId,
        RiskAction action,
        boolean requiresPassword,
        boolean requiresEmailCode
) {}