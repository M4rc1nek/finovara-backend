package com.finovara.authservice.riskverification.service;

import com.finovara.contracts.clientdata.browser.UserBrowser;
import com.finovara.contracts.clientdata.ip.ClientIp;
import com.finovara.contracts.clientdata.location.UserLocation;
import com.finovara.contracts.securitymonitoring.dto.RiskAction;
import com.finovara.contracts.securitymonitoring.dto.RiskEvaluationRequest;
import com.finovara.contracts.securitymonitoring.dto.RiskEvaluationResponse;
import com.finovara.contracts.securitymonitoring.dto.RiskTriggerType;
import com.finovara.authservice.exception.riskverification.precondition.RiskVerificationRequiredException;
import com.finovara.authservice.feignclient.SecurityMonitoringClient;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RiskGuardService {

    private final SecurityMonitoringClient securityMonitoringClient;

    public void guard(Long userId, RiskTriggerType triggerType, String email, String sourceEventId,
                      HttpServletRequest servletRequest) {

        String ipAddress = ClientIp.getClientIpAddress(servletRequest);

        RiskEvaluationRequest request = new RiskEvaluationRequest(
                userId,
                triggerType,
                sourceEventId,
                null,
                null,
                ipAddress,
                UserLocation.getLocationFromIp(ipAddress),
                UserBrowser.getBrowser(servletRequest),
                email
        );

        RiskEvaluationResponse response = securityMonitoringClient.evaluate(request);

        if (response.action() == RiskAction.LOG_ONLY) {
            log.info("Risk check passed for userId={} triggerType={} sourceEventId={}", userId, triggerType, sourceEventId);
            return;
        }

        if (response.isFullyVerified()) {
            log.info("Risk operation already verified for userId={} riskOperationId={}", userId, response.riskOperationId());
            return;
        }

        log.info("Risk verification required for userId={} triggerType={} action={} riskOperationId={}", userId, triggerType, response.action(), response.riskOperationId());

        throw new RiskVerificationRequiredException(response.riskOperationId(), response.action(), response.passwordRequired(), response.emailCodeRequired());
    }
}