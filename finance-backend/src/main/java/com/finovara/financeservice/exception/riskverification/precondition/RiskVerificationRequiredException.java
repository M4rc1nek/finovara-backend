package com.finovara.financeservice.exception.riskverification.precondition;

import com.finovara.contracts.securitymonitoring.model.RiskAction;
import lombok.Getter;

@Getter
public class RiskVerificationRequiredException extends RuntimeException {

    private final Long riskOperationId;
    private final RiskAction action;
    private final boolean requiresPassword;
    private final boolean requiresEmailCode;

    public RiskVerificationRequiredException(Long riskOperationId, RiskAction action, boolean requiresPassword, boolean requiresEmailCode) {
        super("Risk verification required, riskOperationId=" + riskOperationId + ", action=" + action);
        this.riskOperationId = riskOperationId;
        this.action = action;
        this.requiresPassword = requiresPassword;
        this.requiresEmailCode = requiresEmailCode;
    }
}