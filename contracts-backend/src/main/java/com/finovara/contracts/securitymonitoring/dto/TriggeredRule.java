package com.finovara.contracts.securitymonitoring.dto;

import com.finovara.contracts.securitymonitoring.model.RiskRule;

public record TriggeredRule(
        RiskRule rule,
        int points
) {}