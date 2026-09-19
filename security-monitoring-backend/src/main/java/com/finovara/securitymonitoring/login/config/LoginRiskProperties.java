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

    private int unknownDevicePoints;
    private int unknownLocationPoints;
    private int impossibleTravelPoints;
    private int manyKnownDevicesPoints;
    private int partialDeviceMatchPoints;
}
