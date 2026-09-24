package com.finovara.contracts.securitymonitoring.model;

public enum RiskAction {
    LOG_ONLY,
    SOFT_CHALLENGE,
    AUTHORIZATION_REQUIRED,
    FULL_VERIFICATION_REQUIRED
}