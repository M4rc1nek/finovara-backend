package com.finovara.securitymonitoring.riskengine.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@Getter
@Setter
@ConfigurationProperties(prefix = "risk.action")
public class RiskActionThresholds {
    private int challengePoints;
    private int authorizationPoints;
    private int blockPoints;
}