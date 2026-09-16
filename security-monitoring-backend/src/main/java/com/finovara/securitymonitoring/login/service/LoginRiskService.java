package com.finovara.securitymonitoring.login.service;

import com.finovara.securitymonitoring.clientdata.model.ClientData;
import com.finovara.securitymonitoring.login.config.LoginRiskProperties;
import com.finovara.securitymonitoring.login.model.LoginProfile;
import com.finovara.securitymonitoring.riskengine.dto.RiskContext;
import com.finovara.securitymonitoring.riskengine.model.RiskRule;
import com.finovara.securitymonitoring.riskengine.model.RiskTriggerType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoginRiskService {

    private final LoginRiskProperties properties;

    public int evaluate(RiskContext context) {
        if (context.triggerType() != RiskTriggerType.LOGIN || context.loginProfile() == null) {
            return 0;
        }

        log.info("Checking login risk for userId={}", context.userId());

        LoginProfile profile = context.loginProfile();
        List<ClientData> knownDevices = profile.getClientData();

        int totalPoints =
                addPoints(context, RiskRule.UNKNOWN_DEVICE, isUnknownDevice(context, knownDevices), properties.getUnknownDeviceWeight())
                        + addPoints(context, RiskRule.UNKNOWN_LOCATION, isUnknownLocation(context, knownDevices), properties.getUnknownLocationWeight())
                        + addPoints(context, RiskRule.IMPOSSIBLE_TRAVEL, isImpossibleTravel(context, profile), properties.getImpossibleTravelWeight())
                        + addPoints(context, RiskRule.MANY_KNOWN_DEVICES, hasManyKnownDevices(knownDevices), properties.getManyKnownDevicesWeight())
                        + addPoints(context, RiskRule.PARTIAL_DEVICE_MATCH, isPartialDeviceMatch(context, knownDevices), properties.getPartialDeviceMatchWeight());

        log.info("Login risk points for userId={} is {}", context.userId(), totalPoints);

        return totalPoints;
    }

    private boolean isUnknownDevice(RiskContext context, List<ClientData> knownDevices) {
        return !knownDevices.isEmpty()
                && knownDevices.stream()
                .noneMatch(device -> isFullDeviceMatch(device, context));
    }

    private boolean isUnknownLocation(RiskContext context, List<ClientData> knownDevices) {
        return !knownDevices.isEmpty()
                && knownDevices.stream()
                .noneMatch(device -> Objects.equals(device.getKnownLocation(), context.location()));
    }

    private boolean isImpossibleTravel(RiskContext context, LoginProfile profile) {
        return profile.getLastLoginAt() != null
                && profile.getLastLoginLocation() != null
                && !Objects.equals(profile.getLastLoginLocation(), context.location())
                && isWithinTravelWindow(
                profile.getLastLoginAt(),
                Duration.ofHours(properties.getImpossibleTravelHours())
        );
    }

    private boolean hasManyKnownDevices(List<ClientData> knownDevices) {
        return knownDevices.size() >= properties.getManyKnownDevicesThreshold();
    }

    private boolean isPartialDeviceMatch(RiskContext context, List<ClientData> knownDevices) {
        return !knownDevices.isEmpty()
                && knownDevices.stream().noneMatch(device -> isFullDeviceMatch(device, context))
                && knownDevices.stream().anyMatch(device -> isPartialDeviceMatch(device, context));
    }

    private boolean isFullDeviceMatch(ClientData device, RiskContext context) {
        return Objects.equals(device.getKnownIpAddress(), context.ipAddress())
                && Objects.equals(device.getKnownBrowser(), context.browser());
    }

    private boolean isPartialDeviceMatch(ClientData device, RiskContext context) {
        boolean sameIp = Objects.equals(device.getKnownIpAddress(), context.ipAddress());
        boolean sameBrowser = Objects.equals(device.getKnownBrowser(), context.browser());

        return sameIp ^ sameBrowser;
    }

    private boolean isWithinTravelWindow(LocalDateTime since, Duration window) {
        return Duration.between(since, LocalDateTime.now()).compareTo(window) < 0;
    }

    private int addPoints(RiskContext context, RiskRule riskRule, boolean triggered, int points) {
        if (triggered) {
            log.info("Login Risk: Rule triggered for userId={}: {} (+{} points)", context.userId(), riskRule, points);
        }

        return triggered ? points : 0;
    }
}
