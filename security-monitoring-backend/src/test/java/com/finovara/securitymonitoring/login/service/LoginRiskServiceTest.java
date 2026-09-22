package com.finovara.securitymonitoring.login.service;

import com.finovara.contracts.securitymonitoring.dto.RiskTriggerType;
import com.finovara.securitymonitoring.clientdata.model.ClientData;
import com.finovara.securitymonitoring.login.config.LoginRiskProperties;
import com.finovara.securitymonitoring.login.model.LoginProfile;
import com.finovara.securitymonitoring.riskengine.dto.RiskContext;
import com.finovara.securitymonitoring.riskengine.model.RiskRule;
import com.finovara.securitymonitoring.riskengine.model.TriggeredRule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoginRiskServiceTest {

    @Mock
    private LoginRiskProperties properties;

    @InjectMocks
    private LoginRiskService loginRiskService;

    private static final Long USER_ID = 1L;
    private static final String IP = "192.168.1.1";
    private static final String BROWSER = "Chrome";
    private static final String LOCATION = "Warsaw, PL";

    private LoginProfile loginProfile;
    private ClientData device;

    @BeforeEach
    void setUp() {
        loginProfile = mock(LoginProfile.class);
        device = mock(ClientData.class);
    }

    private RiskContext buildContext(RiskTriggerType triggerType, String ipAddress, String location, String browser, LoginProfile profile) {
        return new RiskContext(USER_ID, triggerType, null, null, ipAddress, location, browser, null, profile, null);
    }

    @Nested
    class Evaluate {

        @Test
        void shouldReturnEmptyListWhenTriggerTypeIsNotLogin() {
            RiskContext context = buildContext(RiskTriggerType.PASSWORD_CHANGED, IP, LOCATION, BROWSER, null);

            List<TriggeredRule> result = loginRiskService.evaluate(context);

            assertThat(result).isEmpty();
        }

        @Test
        void shouldReturnEmptyListWhenLoginProfileIsNull() {
            RiskContext context = buildContext(RiskTriggerType.LOGIN, IP, LOCATION, BROWSER, null);

            List<TriggeredRule> result = loginRiskService.evaluate(context);

            assertThat(result).isEmpty();
        }

        @Test
        void shouldReturnEmptyListWhenNoKnownDevicesAndNoLastLogin() {
            when(loginProfile.getClientData()).thenReturn(List.of());
            when(loginProfile.getLastLoginAt()).thenReturn(null);
            when(properties.getManyKnownDevicesThreshold()).thenReturn(5);

            RiskContext context = buildContext(RiskTriggerType.LOGIN, IP, LOCATION, BROWSER, loginProfile);

            List<TriggeredRule> result = loginRiskService.evaluate(context);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    class DeviceRules {

        @Test
        void shouldTriggerUnknownDeviceRuleWhenNoFullMatchAmongKnownDevices() {
            when(device.getKnownIpAddress()).thenReturn("10.0.0.1");
            when(device.getKnownBrowser()).thenReturn("Firefox");
            when(loginProfile.getClientData()).thenReturn(List.of(device));
            when(loginProfile.getLastLoginAt()).thenReturn(null);
            when(properties.getManyKnownDevicesThreshold()).thenReturn(5);
            when(properties.getUnknownDevicePoints()).thenReturn(10);

            RiskContext context = buildContext(RiskTriggerType.LOGIN, IP, null, BROWSER, loginProfile);

            List<TriggeredRule> result = loginRiskService.evaluate(context);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).rule()).isEqualTo(RiskRule.UNKNOWN_DEVICE);
            assertThat(result.get(0).points()).isEqualTo(10);
        }

        @Test
        void shouldTriggerPartialDeviceMatchRuleWhenSameIpDifferentBrowser() {
            when(device.getKnownIpAddress()).thenReturn(IP);
            when(device.getKnownBrowser()).thenReturn("Firefox");
            when(loginProfile.getClientData()).thenReturn(List.of(device));
            when(loginProfile.getLastLoginAt()).thenReturn(null);
            when(properties.getManyKnownDevicesThreshold()).thenReturn(5);
            when(properties.getUnknownDevicePoints()).thenReturn(10);
            when(properties.getPartialDeviceMatchPoints()).thenReturn(4);

            RiskContext context = buildContext(RiskTriggerType.LOGIN, IP, null, BROWSER, loginProfile);

            List<TriggeredRule> result = loginRiskService.evaluate(context);

            assertThat(result).hasSize(2);
            assertThat(result).extracting(TriggeredRule::rule)
                    .containsExactlyInAnyOrder(RiskRule.UNKNOWN_DEVICE, RiskRule.PARTIAL_DEVICE_MATCH);
        }

        @Test
        void shouldTriggerPartialDeviceMatchRuleWhenSameBrowserDifferentIp() {
            when(device.getKnownIpAddress()).thenReturn("10.0.0.1");
            when(device.getKnownBrowser()).thenReturn(BROWSER);
            when(loginProfile.getClientData()).thenReturn(List.of(device));
            when(loginProfile.getLastLoginAt()).thenReturn(null);
            when(properties.getManyKnownDevicesThreshold()).thenReturn(5);
            when(properties.getUnknownDevicePoints()).thenReturn(10);
            when(properties.getPartialDeviceMatchPoints()).thenReturn(4);

            RiskContext context = buildContext(RiskTriggerType.LOGIN, IP, null, BROWSER, loginProfile);

            List<TriggeredRule> result = loginRiskService.evaluate(context);

            assertThat(result).hasSize(2);
            assertThat(result).extracting(TriggeredRule::rule)
                    .containsExactlyInAnyOrder(RiskRule.UNKNOWN_DEVICE, RiskRule.PARTIAL_DEVICE_MATCH);
        }

        @Test
        void shouldNotTriggerUnknownDeviceOrPartialRuleWhenFullMatchExists() {
            when(device.getKnownIpAddress()).thenReturn(IP);
            when(device.getKnownBrowser()).thenReturn(BROWSER);
            when(loginProfile.getClientData()).thenReturn(List.of(device));
            when(loginProfile.getLastLoginAt()).thenReturn(null);
            when(properties.getManyKnownDevicesThreshold()).thenReturn(5);

            RiskContext context = buildContext(RiskTriggerType.LOGIN, IP, null, BROWSER, loginProfile);

            List<TriggeredRule> result = loginRiskService.evaluate(context);

            assertThat(result).isEmpty();
        }

        @Test
        void shouldTriggerManyKnownDevicesRuleWhenThresholdMet() {
            when(device.getKnownIpAddress()).thenReturn(IP);
            when(device.getKnownBrowser()).thenReturn(BROWSER);
            when(loginProfile.getClientData()).thenReturn(List.of(device, device, device));
            when(loginProfile.getLastLoginAt()).thenReturn(null);
            when(properties.getManyKnownDevicesThreshold()).thenReturn(3);
            when(properties.getManyKnownDevicesPoints()).thenReturn(20);

            RiskContext context = buildContext(RiskTriggerType.LOGIN, IP, null, BROWSER, loginProfile);

            List<TriggeredRule> result = loginRiskService.evaluate(context);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).rule()).isEqualTo(RiskRule.MANY_KNOWN_DEVICES);
            assertThat(result.get(0).points()).isEqualTo(20);
        }

        @Test
        void shouldNotTriggerManyKnownDevicesRuleWhenBelowThreshold() {
            when(device.getKnownIpAddress()).thenReturn(IP);
            when(device.getKnownBrowser()).thenReturn(BROWSER);
            when(loginProfile.getClientData()).thenReturn(List.of(device, device));
            when(loginProfile.getLastLoginAt()).thenReturn(null);
            when(properties.getManyKnownDevicesThreshold()).thenReturn(3);

            RiskContext context = buildContext(RiskTriggerType.LOGIN, IP, null, BROWSER, loginProfile);

            List<TriggeredRule> result = loginRiskService.evaluate(context);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    class LocationRules {

        @Test
        void shouldTriggerUnknownLocationRuleWhenNoDeviceMatchesLocation() {
            when(device.getKnownLocation()).thenReturn("Krakow, PL");
            when(loginProfile.getClientData()).thenReturn(List.of(device));
            when(loginProfile.getLastLoginAt()).thenReturn(null);
            when(properties.getManyKnownDevicesThreshold()).thenReturn(100);
            when(properties.getUnknownLocationPoints()).thenReturn(8);

            RiskContext context = buildContext(RiskTriggerType.LOGIN, null, LOCATION, null, loginProfile);

            List<TriggeredRule> result = loginRiskService.evaluate(context);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).rule()).isEqualTo(RiskRule.UNKNOWN_LOCATION);
            assertThat(result.get(0).points()).isEqualTo(8);
        }

        @Test
        void shouldNotTriggerUnknownLocationRuleWhenDeviceMatchesLocation() {
            when(device.getKnownLocation()).thenReturn(LOCATION);
            when(loginProfile.getClientData()).thenReturn(List.of(device));
            when(loginProfile.getLastLoginAt()).thenReturn(null);
            when(properties.getManyKnownDevicesThreshold()).thenReturn(100);

            RiskContext context = buildContext(RiskTriggerType.LOGIN, null, LOCATION, null, loginProfile);

            List<TriggeredRule> result = loginRiskService.evaluate(context);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    class ImpossibleTravelRules {

        @Test
        void shouldTriggerImpossibleTravelRuleWhenWithinWindowAndDifferentLocation() {
            when(loginProfile.getClientData()).thenReturn(List.of());
            when(loginProfile.getLastLoginAt()).thenReturn(LocalDateTime.now().minusHours(2));
            when(loginProfile.getLastLoginLocation()).thenReturn("Krakow, PL");
            when(properties.getManyKnownDevicesThreshold()).thenReturn(5);
            when(properties.getImpossibleTravelHours()).thenReturn(6L);
            when(properties.getImpossibleTravelPoints()).thenReturn(12);

            RiskContext context = buildContext(RiskTriggerType.LOGIN, null, LOCATION, null, loginProfile);

            List<TriggeredRule> result = loginRiskService.evaluate(context);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).rule()).isEqualTo(RiskRule.IMPOSSIBLE_TRAVEL);
            assertThat(result.get(0).points()).isEqualTo(12);
        }

        @Test
        void shouldNotTriggerImpossibleTravelRuleWhenLastLoginAtIsNull() {
            when(loginProfile.getClientData()).thenReturn(List.of());
            when(loginProfile.getLastLoginAt()).thenReturn(null);
            when(properties.getManyKnownDevicesThreshold()).thenReturn(5);

            RiskContext context = buildContext(RiskTriggerType.LOGIN, null, LOCATION, null, loginProfile);

            List<TriggeredRule> result = loginRiskService.evaluate(context);

            assertThat(result).isEmpty();
        }

        @Test
        void shouldNotTriggerImpossibleTravelRuleWhenLastLoginLocationIsNull() {
            when(loginProfile.getClientData()).thenReturn(List.of());
            when(loginProfile.getLastLoginAt()).thenReturn(LocalDateTime.now().minusHours(1));
            when(loginProfile.getLastLoginLocation()).thenReturn(null);
            when(properties.getManyKnownDevicesThreshold()).thenReturn(5);

            RiskContext context = buildContext(RiskTriggerType.LOGIN, null, LOCATION, null, loginProfile);

            List<TriggeredRule> result = loginRiskService.evaluate(context);

            assertThat(result).isEmpty();
        }

        @Test
        void shouldNotTriggerImpossibleTravelRuleWhenLocationMatchesLastLogin() {
            when(loginProfile.getClientData()).thenReturn(List.of());
            when(loginProfile.getLastLoginAt()).thenReturn(LocalDateTime.now().minusHours(1));
            when(loginProfile.getLastLoginLocation()).thenReturn(LOCATION);
            when(properties.getManyKnownDevicesThreshold()).thenReturn(5);

            RiskContext context = buildContext(RiskTriggerType.LOGIN, null, LOCATION, null, loginProfile);

            List<TriggeredRule> result = loginRiskService.evaluate(context);

            assertThat(result).isEmpty();
        }

        @Test
        void shouldNotTriggerImpossibleTravelRuleWhenOutsideWindow() {
            when(loginProfile.getClientData()).thenReturn(List.of());
            when(loginProfile.getLastLoginAt()).thenReturn(LocalDateTime.now().minusHours(10));
            when(loginProfile.getLastLoginLocation()).thenReturn("Krakow, PL");
            when(properties.getManyKnownDevicesThreshold()).thenReturn(5);
            when(properties.getImpossibleTravelHours()).thenReturn(6L);

            RiskContext context = buildContext(RiskTriggerType.LOGIN, null, LOCATION, null, loginProfile);

            List<TriggeredRule> result = loginRiskService.evaluate(context);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    class CombinedRules {

        @Test
        void shouldTriggerAllRulesWhenAllConditionsMet() {
            when(device.getKnownIpAddress()).thenReturn(IP);
            when(device.getKnownBrowser()).thenReturn("Firefox");
            when(device.getKnownLocation()).thenReturn("Krakow, PL");
            when(loginProfile.getClientData()).thenReturn(List.of(device));
            when(loginProfile.getLastLoginAt()).thenReturn(LocalDateTime.now().minusHours(1));
            when(loginProfile.getLastLoginLocation()).thenReturn("Gdansk, PL");

            when(properties.getManyKnownDevicesThreshold()).thenReturn(1);
            when(properties.getUnknownDevicePoints()).thenReturn(10);
            when(properties.getPartialDeviceMatchPoints()).thenReturn(4);
            when(properties.getManyKnownDevicesPoints()).thenReturn(20);
            when(properties.getUnknownLocationPoints()).thenReturn(8);
            when(properties.getImpossibleTravelHours()).thenReturn(6L);
            when(properties.getImpossibleTravelPoints()).thenReturn(12);

            RiskContext context = buildContext(RiskTriggerType.LOGIN, IP, LOCATION, BROWSER, loginProfile);

            List<TriggeredRule> result = loginRiskService.evaluate(context);

            assertThat(result).hasSize(5);
            assertThat(result).extracting(TriggeredRule::rule)
                    .containsExactlyInAnyOrder(
                            RiskRule.UNKNOWN_DEVICE,
                            RiskRule.PARTIAL_DEVICE_MATCH,
                            RiskRule.MANY_KNOWN_DEVICES,
                            RiskRule.UNKNOWN_LOCATION,
                            RiskRule.IMPOSSIBLE_TRAVEL
                    );
        }
    }
}