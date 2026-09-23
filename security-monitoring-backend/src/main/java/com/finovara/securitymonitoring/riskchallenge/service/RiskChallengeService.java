package com.finovara.securitymonitoring.riskchallenge.service;

import com.finovara.contracts.authorization.dto.ConfirmPasswordDto;
import com.finovara.contracts.exception.notfound.RequestedEntityNotFoundException;
import com.finovara.securitymonitoring.exception.badrequest.InvalidChallengeException;
import com.finovara.securitymonitoring.exception.conflict.RiskOperationStateException;
import com.finovara.securitymonitoring.feignclient.AuthBackendClient;
import com.finovara.securitymonitoring.riskchallenge.dto.ChallengeConfirmationResponse;
import com.finovara.securitymonitoring.riskengine.model.RiskOperation;
import com.finovara.securitymonitoring.riskengine.repository.RiskOperationRepository;
import com.finovara.securitymonitoring.util.email.EmailTemplateService;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class RiskChallengeService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String EMAIL_SUBJECT = "Kod autoryzacyjny Finovara";
    private static final String EMAIL_TEMPLATE = "/email/security-authorization-code.html";

    private final RiskOperationRepository riskOperationRepository;
    private final AuthBackendClient authBackendClient;
    private final EmailTemplateService emailTemplateService;

    @Value("${email.secure-code.expires-at-minutes}")
    private int secureCodeExpiresMinutes;

    @Transactional
    public void generateAndSendEmailCode(RiskOperation operation, String recipientEmail) {
        String code = generateCode();

        operation.setEmailCode((code));
        operation.setEmailCodeExpiresAt(LocalDateTime.now().plusMinutes(secureCodeExpiresMinutes));
        riskOperationRepository.save(operation);

        emailTemplateService.sendEmail(recipientEmail, EMAIL_SUBJECT, EMAIL_TEMPLATE, Map.of("code", code));

        log.info("Authorization code sent for riskOperationId={} userId={}", operation.getId(), operation.getUserId());
    }

    @Transactional
    public ChallengeConfirmationResponse confirmPassword(Long riskOperationId, String password) {
        RiskOperation operation = getOperation(riskOperationId);

        if (!operation.requiresPassword()) {
            throw new RiskOperationStateException("Risk operation id=" + riskOperationId + " does not require password confirmation");
        }

        try {
            authBackendClient.verifyPassword(operation.getUserId(), new ConfirmPasswordDto(password));
        } catch (FeignException.Unauthorized | FeignException.Forbidden exception) {
            throw new InvalidChallengeException("Incorrect password for userId=" + operation.getUserId());
        }

        operation.setPasswordConfirmed(true);
        riskOperationRepository.save(operation);

        log.info("Password confirmed for riskOperationId={} userId={}", riskOperationId, operation.getUserId());
        return toConfirmationResponse(operation);
    }

    @Transactional
    public ChallengeConfirmationResponse confirmEmailCode(Long riskOperationId, String code) {
        RiskOperation operation = getOperation(riskOperationId);

        if (!operation.requiresEmailCode()) {
            throw new RiskOperationStateException("Risk operation id=" + riskOperationId + " does not require email code");
        }
        if (operation.getEmailCodeExpiresAt() == null || operation.getEmailCodeExpiresAt().isBefore(LocalDateTime.now())) {
            throw new InvalidChallengeException("Code expired for riskOperationId=" + riskOperationId);
        }
        if (operation.getEmailCode() == null || !operation.getEmailCode().equals(code)) {
            throw new InvalidChallengeException("Invalid code for riskOperationId=" + riskOperationId);
        }

        operation.setEmailCodeConfirmed(true);
        riskOperationRepository.save(operation);

        log.info("Email code confirmed for riskOperationId={} userId={}", riskOperationId, operation.getUserId());
        return toConfirmationResponse(operation);
    }

    private RiskOperation getOperation(Long riskOperationId) {
        return riskOperationRepository.findById(riskOperationId)
                .orElseThrow(() -> new RequestedEntityNotFoundException("Risk operation not found id=" + riskOperationId));
    }

    private ChallengeConfirmationResponse toConfirmationResponse(RiskOperation operation) {
        return new ChallengeConfirmationResponse(operation.getId(), operation.isFullyVerified());
    }

    private String generateCode() {
        return String.format("%06d", RANDOM.nextInt(1_000_000));
    }
}