package com.finovara.securitymonitoring.riskengine.service;

import com.finovara.securitymonitoring.accountchange.repository.AccountChangeProfileRepository;
import com.finovara.securitymonitoring.accountchange.service.AccountChangeRiskService;
import com.finovara.securitymonitoring.login.repository.LoginProfileRepository;
import com.finovara.securitymonitoring.login.service.LoginRiskService;
import com.finovara.securitymonitoring.riskengine.config.RiskActionThresholds;
import com.finovara.securitymonitoring.riskengine.dto.RiskContext;
import com.finovara.securitymonitoring.riskengine.dto.RiskEvaluationRequest;
import com.finovara.securitymonitoring.riskengine.dto.RiskEvaluationResponse;
import com.finovara.securitymonitoring.riskengine.model.RiskAction;
import com.finovara.securitymonitoring.riskengine.model.RiskOperation;
import com.finovara.securitymonitoring.riskengine.repository.RiskOperationRepository;
import com.finovara.securitymonitoring.transaction.repository.TransactionProfileRepository;
import com.finovara.securitymonitoring.transaction.service.TransactionRiskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class RiskEngineService {

    private final TransactionRiskService transactionRiskService;
    private final LoginRiskService loginRiskService;
    private final AccountChangeRiskService accountChangeRiskService;

    private final TransactionProfileRepository transactionProfileRepository;
    private final LoginProfileRepository loginProfileRepository;
    private final AccountChangeProfileRepository accountChangeProfileRepository;
    private final RiskOperationRepository riskOperationRepository;
    private final RiskActionThresholds thresholds;

    @Transactional
    public RiskEvaluationResponse evaluate(RiskEvaluationRequest request) {
        log.info("Starting risk check for userId={} type={}", request.userId(), request.triggerType());

        return riskOperationRepository.findBySourceEventId(request.sourceEventId())
                .map(this::toResponse)
                .orElseGet(() -> computeAndSave(request));
    }

    private RiskEvaluationResponse computeAndSave(RiskEvaluationRequest request) {
        RiskContext context = buildContext(request);

        int transactionPoints = transactionRiskService.evaluate(context);
        int loginPoints = loginRiskService.evaluate(context);
        int accountChangePoints = accountChangeRiskService.evaluate(context);

        int totalScore = Math.clamp(transactionPoints + loginPoints + accountChangePoints, 0, 100);

        RiskAction action = resolveAction(totalScore);

        log.info("Risk check finished for userId={} score={} action={}", request.userId(), totalScore, action);

        RiskOperation operation = riskOperationRepository.save(RiskOperation.builder()
                .sourceEventId(request.sourceEventId())
                .triggerType(request.triggerType())
                .score(totalScore)
                .action(action)
                .operationDate(LocalDate.now())
                .createdAt(LocalDateTime.now())
                .userId(request.userId())
                .build());

        return toResponse(operation);
    }

    private RiskAction resolveAction(int score) {
        if (score >= thresholds.getBlockPoints()) return RiskAction.BLOCK_AND_REVIEW;
        if (score >= thresholds.getAuthorizationPoints()) return RiskAction.AUTHORIZATION_REQUIRED;
        if (score >= thresholds.getChallengePoints()) return RiskAction.SOFT_CHALLENGE;
        return RiskAction.LOG_ONLY;
    }

    private RiskContext buildContext(RiskEvaluationRequest request) {
        return new RiskContext(
                request.userId(),
                request.triggerType(),
                request.amount(),
                request.category(),
                request.ipAddress(),
                request.location(),
                request.browser(),
                transactionProfileRepository.findByUserId(request.userId()).orElse(null),
                loginProfileRepository.findByUserId(request.userId()).orElse(null),
                accountChangeProfileRepository.findByUserId(request.userId()).orElse(null)
        );
    }

    private RiskEvaluationResponse toResponse(RiskOperation operation) {
        return new RiskEvaluationResponse(operation.getId(), operation.getScore(), operation.getAction());
    }
}