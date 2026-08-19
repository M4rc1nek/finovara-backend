package com.finovara.activitylogservice.activitylog.accountactivity.sharedaccount.service;

import com.finovara.activitylogservice.activitylog.accountactivity.core.AccountActivityCore;
import com.finovara.activitylogservice.activitylog.accountactivity.sharedaccount.dto.SharedAccountActivityDto;
import com.finovara.activitylogservice.activitylog.accountactivity.sharedaccount.mapper.SharedAccountActivityMapper;
import com.finovara.activitylogservice.activitylog.accountactivity.sharedaccount.model.SharedAccountActivity;
import com.finovara.activitylogservice.activitylog.accountactivity.sharedaccount.repository.SharedAccountActivityRepository;
import com.finovara.activitylogservice.feignclient.AuthBackendClient;
import com.finovara.contracts.authorization.dto.ConfirmPasswordDto;
import com.finovara.contracts.datadeletable.UserDataDeletable;
import com.finovara.contracts.activity.event.sharedaccount.SharedAccountActivityEvent;
import com.finovara.contracts.model.SortType;
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
public class SharedAccountActivityService extends AccountActivityCore<SharedAccountActivity, SharedAccountActivityDto> implements UserDataDeletable {

    @Value("${user-activity.shared-account.page-size}")
    private int pageSize;

    private final SharedAccountActivityRepository sharedAccountActivityRepository;
    private final SharedAccountActivityMapper sharedAccountActivityMapper;
    private final AuthBackendClient authBackendClient;


    @Transactional
    public void handleEvent(SharedAccountActivityEvent event) {
        SharedAccountActivity revenueActivity = SharedAccountActivity.builder()
                .userId(event.userId())
                .type(event.type())
                .refundedBalance(event.refundedBalance())
                .coFounderUsername(event.coFounderUsername())
                .coFounderEmail(event.coFounderEmail())
                .createdAt(event.occurredAt())
                .build();

        sharedAccountActivityRepository.save(revenueActivity);
        log.info("Created shared account activity. Type: {}, userId: {}", event.type(), event.userId());
    }

    public List<SharedAccountActivityDto> getSharedAccountActivity(Long userId, SortType sort) {
        return getActivities(userId, sort, pageSize);
    }

    public void confirmPassword(Long userId, ConfirmPasswordDto confirmPasswordDto) {
        authBackendClient.verifyPassword(userId, confirmPasswordDto);
    }

    @Override
    protected List<SharedAccountActivity> getRepositoryFindByUserId(Long userId, Pageable pageable) {
        return sharedAccountActivityRepository.findByUserId(userId, pageable);
    }

    @Override
    protected SharedAccountActivityDto mapToDto(SharedAccountActivity entity) {
        return sharedAccountActivityMapper.mapToSharedAccountActivity(entity);
    }

    @Override
    @Transactional
    public void deleteByUserId(Long userId) {
        sharedAccountActivityRepository.deleteByUserId(userId);
        log.info("Deleted shared account activity for userId={}", userId);
    }
}
