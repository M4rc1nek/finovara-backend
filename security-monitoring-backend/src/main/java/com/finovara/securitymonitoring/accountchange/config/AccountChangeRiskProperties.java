package com.finovara.securitymonitoring.accountchange.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@Getter
@Setter
@ConfigurationProperties(prefix = "risk.account-change")
public class AccountChangeRiskProperties {
    private long repeatChangeMinutes;
    private long identityChangeMinutes;
    private long newAccountDays;
    private long newAccountChangeCount;

    private int repeatChangePoints;
    private int identityChangePoints;
    private int newAccountPoints;
}