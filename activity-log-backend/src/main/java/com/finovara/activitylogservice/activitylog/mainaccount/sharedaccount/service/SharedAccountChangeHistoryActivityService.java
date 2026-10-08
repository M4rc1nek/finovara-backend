package com.finovara.activitylogservice.activitylog.mainaccountactivity.sharedaccount.service;

import com.finovara.activitylogservice.activitylog.mainaccountactivity.core.AccountActivityCore;
import com.finovara.activitylogservice.activitylog.mainaccountactivity.sharedaccount.dto.SharedAccountChangeHistoryActivityDto;
import com.finovara.activitylogservice.activitylog.mainaccountactivity.sharedaccount.mapper.SharedAccountActivityMapper;
import com.finovara.activitylogservice.activitylog.mainaccountactivity.sharedaccount.model.SharedAccountChangeHistoryActivity;
import com.finovara.activitylogservice.activitylog.mainaccountactivity.sharedaccount.repository.SharedAccountChangeHistoryActivityRepository;
import com.finovara.activitylogservice.feignclient.AuthBackendClient;
import com.finovara.contracts.user.authorization.dto.ConfirmPasswordDto;
import com.finovara.contracts.user.datadeletable.UserDataDeletable;
import com.finovara.contracts.mainaccount.activity.event.sharedaccount.SharedAccountActivityEvent;
import com.finovara.contracts.util.SortType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SharedAccountChangeHistoryActivityService extends AccountActivityCore<SharedAccountChangeHistoryActivity, SharedAccountChangeHistoryActivityDto> implements UserDataDeletable {

    @Value("${user-activity.shared-account.page-size}")
    private int pageSize;

    private final SharedAccountChangeHistoryActivityRepository sharedAccountChangeHistoryActivityRepository;
    private final SharedAccountActivityMapper sharedAccountActivityMapper;
    private final AuthBackendClient authBackendClient;


    @Transactional
    public void handleEvent(SharedAccountActivityEvent event) {
        SharedAccountChangeHistoryActivity revenueActivity = SharedAccountChangeHistoryActivity.builder()
                .userId(event.userId())
                .type(event.type())
                .refundedBalance(event.refundedBalance())
                .coFounderUsername(event.coFounderUsername())
                .coFounderEmail(event.coFounderEmail())
                .createdAt(event.occurredAt())
                .build();

        sharedAccountChangeHistoryActivityRepository.save(revenueActivity);
        log.info("Created shared account activity. Type: {}, userId: {}", event.type(), event.userId());
    }

    public List<SharedAccountChangeHistoryActivityDto> getSharedAccountActivity(Long userId, SortType sort) {
        return getActivities(userId, sort, pageSize);
    }

    public void confirmPassword(Long userId, ConfirmPasswordDto confirmPasswordDto) {
        authBackendClient.verifyPassword(userId, confirmPasswordDto);
    }

    @Override
    protected List<SharedAccountChangeHistoryActivity> getRepositoryFindByUserId(Long userId, Pageable pageable) {
        return sharedAccountChangeHistoryActivityRepository.findByUserId(userId, pageable);
    }

    @Override
    protected SharedAccountChangeHistoryActivityDto mapToDto(SharedAccountChangeHistoryActivity entity) {
        return sharedAccountActivityMapper.mapToSharedAccountActivity(entity);
    }

    @Override
    @Transactional
    public void deleteByUserId(Long userId) {
        sharedAccountChangeHistoryActivityRepository.deleteByUserId(userId);
        log.info("Deleted shared account activity for userId={}", userId);
    }
}
