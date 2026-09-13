package com.finovara.securitymonitoring.riskengine.model;

public enum RiskAction {
    LOG_ONLY,
    SOFT_CHALLENGE,
    AUTHORIZATION_REQUIRED,
    BLOCK_AND_REVIEW
}