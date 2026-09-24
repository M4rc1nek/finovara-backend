package com.finovara.financeservice.exception.riskverification;

import com.finovara.contracts.securitymonitoring.model.RiskAction;

public record RiskVerificationRequiredResponse(
        Long riskOperationId,
        RiskAction action,
        boolean requiresPassword,
        boolean requiresEmailCode
) {}