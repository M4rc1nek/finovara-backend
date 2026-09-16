package com.finovara.securitymonitoring.login.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@Getter
@Setter
@ConfigurationProperties(prefix = "risk.login")
public class LoginRiskProperties {

    private int manyKnownDevicesThreshold;
    private long impossibleTravelHours;

    private int unknownDeviceWeight;
    private int unknownLocationWeight;
    private int impossibleTravelWeight;
    private int manyKnownDevicesWeight;
    private int partialDeviceMatchWeight;
}
