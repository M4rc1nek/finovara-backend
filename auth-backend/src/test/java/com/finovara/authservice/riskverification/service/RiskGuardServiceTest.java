package com.finovara.authservice.riskverification.service;

import com.finovara.authservice.exception.riskverification.precondition.RiskVerificationRequiredException;
import com.finovara.authservice.feignclient.SecurityMonitoringClient;
import com.finovara.contracts.clientdata.browser.UserBrowser;
import com.finovara.contracts.clientdata.location.UserLocation;
import com.finovara.contracts.securitymonitoring.dto.RiskAction;
import com.finovara.contracts.securitymonitoring.dto.RiskEvaluationRequest;
import com.finovara.contracts.securitymonitoring.dto.RiskEvaluationResponse;
import com.finovara.contracts.securitymonitoring.dto.RiskTriggerType;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RiskGuardServiceTest {

    @Mock
    private SecurityMonitoringClient securityMonitoringClient;

    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private RiskGuardService riskGuardService;

    private static final Long USER_ID = 1L;
    private static final RiskTriggerType TRIGGER_TYPE = RiskTriggerType.PASSWORD_CHANGED;
    private static final String EMAIL = "user@test.com";
    private static final String SOURCE_EVENT_ID = "source-event-id";
    private static final String IP_ADDRESS = "192.168.1.100";
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/120.0";

    private RiskEvaluationResponse response;

    @BeforeEach
    void setUp() {
        response = mock(RiskEvaluationResponse.class);

        when(request.getRemoteAddr()).thenReturn(IP_ADDRESS);
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getHeader("User-Agent")).thenReturn(USER_AGENT);
    }

    private RiskAction nonLogOnlyAction() {
        return java.util.Arrays.stream(RiskAction.values()).filter(action -> action != RiskAction.LOG_ONLY).findFirst().orElseThrow();
    }

    @Nested
    class Guard {

        @Test
        void shouldNotThrowWhenActionIsLogOnly() {
            when(response.action()).thenReturn(RiskAction.LOG_ONLY);
            when(securityMonitoringClient.evaluate(any(RiskEvaluationRequest.class))).thenReturn(response);

            riskGuardService.guard(USER_ID, TRIGGER_TYPE, EMAIL, SOURCE_EVENT_ID, request);

            verify(securityMonitoringClient).evaluate(any(RiskEvaluationRequest.class));
        }

        @Test
        void shouldNotThrowWhenOperationIsFullyVerified() {
            when(response.action()).thenReturn(nonLogOnlyAction());
            when(response.isFullyVerified()).thenReturn(true);
            when(securityMonitoringClient.evaluate(any(RiskEvaluationRequest.class))).thenReturn(response);

            riskGuardService.guard(USER_ID, TRIGGER_TYPE, EMAIL, SOURCE_EVENT_ID, request);

            verify(securityMonitoringClient).evaluate(any(RiskEvaluationRequest.class));
        }

        @Test
        void shouldThrowRiskVerificationRequiredExceptionWhenNotFullyVerifiedAndActionRequiresVerification() {
            RiskAction action = nonLogOnlyAction();

            when(response.action()).thenReturn(action);
            when(response.isFullyVerified()).thenReturn(false);
            when(securityMonitoringClient.evaluate(any(RiskEvaluationRequest.class))).thenReturn(response);

            assertThrows(RiskVerificationRequiredException.class, () -> riskGuardService.guard(USER_ID, TRIGGER_TYPE, EMAIL, SOURCE_EVENT_ID, request));
        }

        @Test
        void shouldCheckFullyVerifiedOnlyWhenActionIsNotLogOnly() {
            when(response.action()).thenReturn(RiskAction.LOG_ONLY);
            when(securityMonitoringClient.evaluate(any(RiskEvaluationRequest.class))).thenReturn(response);

            riskGuardService.guard(USER_ID, TRIGGER_TYPE, EMAIL, SOURCE_EVENT_ID, request);

            verify(response, org.mockito.Mockito.never()).isFullyVerified();
        }

        @Test
        void shouldPassUserIdTriggerTypeSourceEventIdIpLocationBrowserAndEmailToEvaluationRequest() {
            when(response.action()).thenReturn(RiskAction.LOG_ONLY);
            when(securityMonitoringClient.evaluate(any(RiskEvaluationRequest.class))).thenReturn(response);

            riskGuardService.guard(USER_ID, TRIGGER_TYPE, EMAIL, SOURCE_EVENT_ID, request);

            ArgumentCaptor<RiskEvaluationRequest> captor = ArgumentCaptor.forClass(RiskEvaluationRequest.class);

            verify(securityMonitoringClient).evaluate(captor.capture());

            RiskEvaluationRequest capturedRequest = captor.getValue();

            assertThat(capturedRequest.userId()).isEqualTo(USER_ID);
            assertThat(capturedRequest.triggerType()).isEqualTo(TRIGGER_TYPE);
            assertThat(capturedRequest.sourceEventId()).isEqualTo(SOURCE_EVENT_ID);
            assertThat(capturedRequest.email()).isEqualTo(EMAIL);

            assertThat(capturedRequest.ipAddress()).isEqualTo(IP_ADDRESS);
            assertThat(capturedRequest.location()).isEqualTo(UserLocation.getLocationFromIp(IP_ADDRESS));
            assertThat(capturedRequest.browser()).isEqualTo(UserBrowser.getBrowser(request));
        }

        @Test
        void shouldSetAmountAndCategoryToNull() {
            when(response.action()).thenReturn(RiskAction.LOG_ONLY);
            when(securityMonitoringClient.evaluate(any(RiskEvaluationRequest.class))).thenReturn(response);

            riskGuardService.guard(USER_ID, TRIGGER_TYPE, EMAIL, SOURCE_EVENT_ID, request);

            ArgumentCaptor<RiskEvaluationRequest> captor = ArgumentCaptor.forClass(RiskEvaluationRequest.class);

            verify(securityMonitoringClient).evaluate(captor.capture());

            RiskEvaluationRequest capturedRequest = captor.getValue();

            assertThat(capturedRequest.amount()).isNull();
            assertThat(capturedRequest.category()).isNull();
        }

        @Test
        void shouldAcceptNullSourceEventIdWhenNotProvided() {
            when(response.action()).thenReturn(RiskAction.LOG_ONLY);
            when(securityMonitoringClient.evaluate(any(RiskEvaluationRequest.class))).thenReturn(response);

            riskGuardService.guard(USER_ID, TRIGGER_TYPE, EMAIL, null, request);

            ArgumentCaptor<RiskEvaluationRequest> captor = ArgumentCaptor.forClass(RiskEvaluationRequest.class);

            verify(securityMonitoringClient).evaluate(captor.capture());

            assertThat(captor.getValue().sourceEventId()).isNull();
        }

        @Test
        void shouldEvaluateExactlyOnceRegardlessOfOutcome() {
            when(response.action()).thenReturn(RiskAction.LOG_ONLY);
            when(securityMonitoringClient.evaluate(any(RiskEvaluationRequest.class))).thenReturn(response);

            riskGuardService.guard(USER_ID, TRIGGER_TYPE, EMAIL, SOURCE_EVENT_ID, request);

            verify(securityMonitoringClient, org.mockito.Mockito.times(1)).evaluate(any(RiskEvaluationRequest.class));
        }

        @Test
        void shouldUseClientIpFromRequest() {
            when(request.getRemoteAddr()).thenReturn(IP_ADDRESS);
            when(response.action()).thenReturn(RiskAction.LOG_ONLY);
            when(securityMonitoringClient.evaluate(any(RiskEvaluationRequest.class))).thenReturn(response);

            riskGuardService.guard(USER_ID, TRIGGER_TYPE, EMAIL, SOURCE_EVENT_ID, request);

            ArgumentCaptor<RiskEvaluationRequest> captor = ArgumentCaptor.forClass(RiskEvaluationRequest.class);

            verify(securityMonitoringClient).evaluate(captor.capture());

            assertThat(captor.getValue().ipAddress()).isEqualTo(IP_ADDRESS);
        }
    }
}