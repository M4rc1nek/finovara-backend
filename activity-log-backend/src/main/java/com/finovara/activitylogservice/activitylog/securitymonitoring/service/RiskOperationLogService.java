package com.finovara.activitylogservice.activitylog.securitymonitoring.service;

import com.finovara.activitylogservice.activitylog.accountactivity.core.AccountActivityCore;
import com.finovara.activitylogservice.activitylog.securitymonitoring.dto.RiskOperationActivityDto;
import com.finovara.activitylogservice.activitylog.securitymonitoring.mapper.RiskOperationActivityMapper;
import com.finovara.activitylogservice.activitylog.securitymonitoring.model.RiskOperationActivity;
import com.finovara.activitylogservice.activitylog.securitymonitoring.model.RiskRuleCollectionActivity;
import com.finovara.activitylogservice.activitylog.securitymonitoring.repository.RiskOperationActivityRepository;
import com.finovara.activitylogservice.feignclient.AuthBackendClient;
import com.finovara.contracts.activity.event.securitymonitoring.RiskOperationCreatedEvent;
import com.finovara.contracts.authorization.dto.ConfirmPasswordDto;
import com.finovara.contracts.datadeletable.UserDataDeletable;
import com.finovara.contracts.model.SortType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RiskOperationLogService extends AccountActivityCore<RiskOperationActivity, RiskOperationActivityDto> implements UserDataDeletable {

    @Value("${user-activity.risk-operation.page-size}")
    private int pageSize;

    private final RiskOperationActivityRepository riskOperationActivityRepository;
    private final RiskOperationActivityMapper riskOperationActivityMapper;
    private final AuthBackendClient authBackendClient;

    @Transactional
    public void handleEvent(RiskOperationCreatedEvent event) {
        if (riskOperationActivityRepository.existsBySourceEventId(event.sourceEventId())) {
            log.info("Duplicate risk event skipped, sourceEventId={}", event.sourceEventId());
            return;
        }

        RiskOperationActivity activity = RiskOperationActivity.builder()
                .userId(event.userId())
                .sourceEventId(event.sourceEventId())
                .triggerType(event.triggerType())
                .score(event.score())
                .action(event.action())
                .operationDate(event.operationDate())
                .createdAt(event.createdAt())
                .riskRuleCollectionActivities(new ArrayList<>())
                .build();

        event.riskRules().forEach(rule ->
                activity.getRiskRuleCollectionActivities().add(
                        RiskRuleCollectionActivity.builder()
                                .riskRule(rule)
                                .riskOperationActivity(activity)
                                .build()));

        riskOperationActivityRepository.save(activity);
        log.info("Risk operation activity has been saved.");
    }

    public void confirmPassword(Long userId, ConfirmPasswordDto confirmPasswordDto) {
        authBackendClient.verifyPassword(userId, confirmPasswordDto);
    }

    @Transactional(readOnly = true)
    public List<RiskOperationActivityDto> getRiskActivity(Long userId, SortType sort) {
        return getActivities(userId, sort, pageSize);
    }

    @Override
    protected List<RiskOperationActivity> getRepositoryFindByUserId(Long userId, Pageable pageable) {
        return riskOperationActivityRepository.findByUserId(userId, pageable);
    }

    @Override
    protected RiskOperationActivityDto mapToDto(RiskOperationActivity riskOperationActivity) {
        return riskOperationActivityMapper.mapToRiskOperationActivity(riskOperationActivity);
    }

    @Override
    @Transactional
    public void deleteByUserId(Long userId) {
        riskOperationActivityRepository.deleteByUserId(userId);
    }
}