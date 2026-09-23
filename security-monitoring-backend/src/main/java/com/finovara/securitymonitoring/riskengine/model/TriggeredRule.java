package com.finovara.securitymonitoring.riskengine.model;

public record TriggeredRule(
        RiskRule rule,
        int points) {}