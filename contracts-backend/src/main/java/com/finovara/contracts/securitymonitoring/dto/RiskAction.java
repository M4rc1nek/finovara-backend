package com.finovara.contracts.securitymonitoring.dto;

public enum RiskAction {
    LOG_ONLY,
    SOFT_CHALLENGE,
    AUTHORIZATION_REQUIRED,
    FULL_VERIFICATION_REQUIRED
}