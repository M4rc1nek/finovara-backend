package com.finovara.securitymonitoring.riskengine.service;

import com.finovara.contracts.securitymonitoring.dto.RiskTriggerType;
import com.finovara.securitymonitoring.accountchange.repository.AccountChangeProfileRepository;
import com.finovara.securitymonitoring.accountchange.service.AccountChangeRiskService;
import com.finovara.securitymonitoring.login.repository.LoginProfileRepository;
import com.finovara.securitymonitoring.login.service.LoginRiskService;
import com.finovara.securitymonitoring.riskchallenge.service.RiskChallengeService;
import com.finovara.securitymonitoring.riskengine.config.RiskActionThresholds;
import com.finovara.securitymonitoring.riskengine.dto.RiskContext;
import com.finovara.contracts.securitymonitoring.dto.RiskEvaluationRequest;
import com.finovara.contracts.securitymonitoring.dto.RiskEvaluationResponse;
import com.finovara.contracts.securitymonitoring.dto.RiskAction;
import com.finovara.securitymonitoring.riskengine.model.RiskOperation;
import com.finovara.securitymonitoring.riskengine.model.RiskRuleCollection;
import com.finovara.securitymonitoring.riskengine.model.TriggeredRule;
import com.finovara.securitymonitoring.riskengine.repository.RiskOperationRepository;
import com.finovara.securitymonitoring.transaction.repository.TransactionProfileRepository;
import com.finovara.securitymonitoring.transaction.service.TransactionRiskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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
    private final RiskChallengeService riskChallengeService;

    @Transactional
    public RiskEvaluationResponse evaluate(RiskEvaluationRequest request) {
        log.info("Starting risk check for userId={} type={}", request.userId(), request.triggerType());

        return riskOperationRepository.findBySourceEventId(request.sourceEventId())
                .map(this::toResponse)
                .orElseGet(() -> computeAndSave(request));
    }

    private RiskEvaluationResponse computeAndSave(RiskEvaluationRequest request) {
        RiskContext context = buildContext(request);

        List<TriggeredRule> triggered = new ArrayList<>();
        triggered.addAll(transactionRiskService.evaluate(context));
        triggered.addAll(loginRiskService.evaluate(context));
        triggered.addAll(accountChangeRiskService.evaluate(context));

        int totalScore = Math.clamp(triggered.stream().mapToInt(TriggeredRule::points).sum(), 0, 100);
        RiskAction action = resolveAction(request.triggerType(), totalScore);

        log.info("Risk check finished for userId={} score={} action={}", request.userId(), totalScore, action);

        RiskOperation operation = RiskOperation.builder()
                .sourceEventId(request.sourceEventId())
                .triggerType(request.triggerType())
                .score(totalScore)
                .action(action)
                .operationDate(LocalDate.now())
                .createdAt(LocalDateTime.now())
                .userId(request.userId())
                .build();

        operation.setRiskRuleCollections(buildRuleCollections(triggered, operation));

        riskOperationRepository.save(operation);

        if (operation.requiresEmailCode()) {
            riskChallengeService.generateAndSendEmailCode(operation, request.email());
        }

        return toResponse(operation);
    }

    private List<RiskRuleCollection> buildRuleCollections(List<TriggeredRule> triggered, RiskOperation operation) {
        return triggered.stream()
                .map(triggeredRule -> RiskRuleCollection.builder()
                        .riskRule(triggeredRule.rule())
                        .scorePerRule(triggeredRule.points())
                        .riskOperation(operation)
                        .build())
                .toList();
    }

    private RiskAction resolveAction(RiskTriggerType triggerType, int score) {
        if (triggerType == RiskTriggerType.LOGIN) {
            return score > thresholds.getLoginLogOnlyMaxPoints() ? RiskAction.AUTHORIZATION_REQUIRED : RiskAction.LOG_ONLY;
        }

        if (score >= thresholds.getFullVerificationPoints()) return RiskAction.FULL_VERIFICATION_REQUIRED;
        if (score >= thresholds.getAuthorizationPoints()) return RiskAction.AUTHORIZATION_REQUIRED;
        if (score >= thresholds.getSoftChallengePoints()) return RiskAction.SOFT_CHALLENGE;
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
        return new RiskEvaluationResponse(
                operation.getId(),
                operation.getScore(),
                operation.getAction(),
                operation.requiresPassword(),
                operation.requiresEmailCode(),
                operation.isFullyVerified()
        );
    }
}