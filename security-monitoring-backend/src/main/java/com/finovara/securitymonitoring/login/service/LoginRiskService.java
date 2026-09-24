package com.finovara.securitymonitoring.login.service;

import com.finovara.securitymonitoring.clientdata.model.ClientData;
import com.finovara.securitymonitoring.login.config.LoginRiskProperties;
import com.finovara.securitymonitoring.login.model.LoginProfile;
import com.finovara.securitymonitoring.riskengine.dto.RiskContext;
import com.finovara.contracts.securitymonitoring.model.RiskRule;
import com.finovara.contracts.securitymonitoring.dto.TriggeredRule;
import com.finovara.contracts.securitymonitoring.model.RiskTriggerType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoginRiskService {

    private final LoginRiskProperties properties;

    public List<TriggeredRule> evaluate(RiskContext context) {
        if (context.triggerType() != RiskTriggerType.LOGIN || context.loginProfile() == null) {
            return List.of();
        }

        log.info("Checking login risk for userId={}", context.userId());

        LoginProfile profile = context.loginProfile();
        List<ClientData> knownDevices = profile.getClientData();

        List<TriggeredRule> triggered = new ArrayList<>();
        triggered.addAll(evaluateDeviceRules(context, knownDevices));
        triggered.addAll(evaluateLocationRules(context, profile, knownDevices));

        log.info("Login risk points for userId={} is {}", context.userId(), triggered.stream().mapToInt(TriggeredRule::points).sum());

        return triggered;
    }

    private List<TriggeredRule> evaluateDeviceRules(RiskContext context, List<ClientData> knownDevices) {
        List<TriggeredRule> rules = new ArrayList<>();

        boolean hasFullMatch = knownDevices.stream().anyMatch(device -> isFullDeviceMatch(device, context));

        if (!knownDevices.isEmpty() && !hasFullMatch) {
            rules.add(trigger(context, RiskRule.UNKNOWN_DEVICE, properties.getUnknownDevicePoints()));

            boolean hasPartialMatch = knownDevices.stream().anyMatch(device -> isPartialDeviceMatch(device, context));
            if (hasPartialMatch) {
                rules.add(trigger(context, RiskRule.PARTIAL_DEVICE_MATCH, properties.getPartialDeviceMatchPoints()));
            }
        }

        if (knownDevices.size() >= properties.getManyKnownDevicesThreshold()) {
            rules.add(trigger(context, RiskRule.MANY_KNOWN_DEVICES, properties.getManyKnownDevicesPoints()));
        }

        return rules;
    }

    private List<TriggeredRule> evaluateLocationRules(RiskContext context, LoginProfile profile, List<ClientData> knownDevices) {
        List<TriggeredRule> rules = new ArrayList<>();

        boolean isKnownLocation = knownDevices.stream()
                .anyMatch(device -> Objects.equals(device.getKnownLocation(), context.location()));
        if (!knownDevices.isEmpty() && !isKnownLocation) {
            rules.add(trigger(context, RiskRule.UNKNOWN_LOCATION, properties.getUnknownLocationPoints()));
        }

        boolean isImpossibleTravel = profile.getLastLoginAt() != null
                && profile.getLastLoginLocation() != null
                && !Objects.equals(profile.getLastLoginLocation(), context.location())
                && isWithinTravelWindow(profile.getLastLoginAt(), Duration.ofHours(properties.getImpossibleTravelHours()));
        if (isImpossibleTravel) {
            rules.add(trigger(context, RiskRule.IMPOSSIBLE_TRAVEL, properties.getImpossibleTravelPoints()));
        }

        return rules;
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

    private TriggeredRule trigger(RiskContext context, RiskRule rule, int points) {
        log.info("Login Risk: Rule triggered for userId={}: {} (+{} points)", context.userId(), rule, points);
        return new TriggeredRule(rule, points);
    }
}