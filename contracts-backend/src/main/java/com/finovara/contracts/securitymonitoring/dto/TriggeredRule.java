package com.finovara.securitymonitoring.riskengine.model;

import com.finovara.contracts.securitymonitoring.dto.RiskRule;

public record TriggeredRule(
        RiskRule rule,
        int points
) {}