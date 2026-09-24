package com.finovara.financeservice.riskverification.service;


import com.finovara.contracts.clientdata.browser.UserBrowser;
import com.finovara.contracts.clientdata.ip.ClientIp;
import com.finovara.contracts.clientdata.location.UserLocation;
import com.finovara.contracts.securitymonitoring.model.RiskAction;
import com.finovara.contracts.securitymonitoring.dto.RiskEvaluationRequest;
import com.finovara.contracts.securitymonitoring.dto.RiskEvaluationResponse;
import com.finovara.contracts.securitymonitoring.model.RiskTriggerType;
import com.finovara.financeservice.exception.riskverification.precondition.RiskVerificationRequiredException;
import com.finovara.financeservice.feignclient.SecurityMonitoringClient;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class RiskGuardService {

    private final SecurityMonitoringClient securityMonitoringClient;

    public void guard(Long userId, RiskTriggerType triggerType, BigDecimal amount, String category, String email,
                      String sourceEventId, HttpServletRequest servletRequest) {

        String ipAddress = ClientIp.getClientIpAddress(servletRequest);

        RiskEvaluationRequest request = new RiskEvaluationRequest(
                userId,
                triggerType,
                sourceEventId,
                amount,
                category,
                ipAddress,
                UserLocation.getLocationFromIp(ipAddress),
                UserBrowser.getBrowser(servletRequest),
                email
        );

        RiskEvaluationResponse response = securityMonitoringClient.evaluate(request);

        if (response.action() == RiskAction.LOG_ONLY) {
            log.info("Risk check passed for userId={} sourceEventId={}", userId, sourceEventId);
            return;
        }

        if (response.isFullyVerified()) {
            log.info("Risk operation already verified for userId={} riskOperationId={}", userId, response.riskOperationId());
            return;
        }

        log.info("Risk verification required for userId={} action={} riskOperationId={}", userId, response.action(), response.riskOperationId());

        throw new RiskVerificationRequiredException(response.riskOperationId(), response.action(), response.passwordRequired(), response.emailCodeRequired());
    }
}