package com.finovara.activitylogservice.activitylog.accountactivity.sharedaccount.service;

import com.finovara.activitylogservice.activitylog.accountactivity.core.AccountActivityCore;
import com.finovara.activitylogservice.activitylog.accountactivity.sharedaccount.dto.SharedAccountActivityDto;
import com.finovara.activitylogservice.activitylog.accountactivity.sharedaccount.mapper.SharedAccountActivityMapper;
import com.finovara.activitylogservice.activitylog.accountactivity.sharedaccount.model.SharedAccountActivity;
import com.finovara.activitylogservice.activitylog.accountactivity.sharedaccount.repository.SharedAccountActivityRepository;
import com.finovara.contracts.datadeletable.UserDataDeletable;
import com.finovara.contracts.event.activity.revenue.RevenueActivityEvent;
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
public class RevenueActivityService extends AccountActivityCore<SharedAccountActivity, SharedAccountActivityDto> implements UserDataDeletable {

    @Value("${user-activity.revenue.page-size}")
    private int pageSize;

    private final SharedAccountActivityRepository revenueActivityRepository;
    private final SharedAccountActivityMapper sharedAccountActivityMapper;

    @Transactional
    public void handleEvent(RevenueActivityEvent event) {
        SharedAccountActivity revenueActivity = SharedAccountActivity.builder()
                .userId(event.userId())
                .type(event.type())
                .amount(event.amount())
                .category(event.category())
                .previousAmount(event.previousAmount())
                .previousCategory(event.previousCategory())
                .createdAt(event.occurredAt())
                .build();

        revenueActivityRepository.save(revenueActivity);
        log.info("Created activity type: {}, userId: {}", event.type(), event.userId());
    }

    public List<SharedAccountActivityDto> getRevenueActivity(Long userId, SortType sort) {
        return getActivities(userId, sort, pageSize);
    }

    @Override
    protected List<SharedAccountActivity> getRepositoryFindByUserId(Long userId, Pageable pageable) {
        return revenueActivityRepository.findByUserId(userId, pageable);
    }

    @Override
    protected SharedAccountActivityDto mapToDto(SharedAccountActivity entity) {
        return sharedAccountActivityMapper.mapToRevenueActivity(entity);
    }

    @Override
    @Transactional
    public void deleteByUserId(Long userId) {
        revenueActivityRepository.deleteByUserId(userId);
        log.info("Deleted revenue activity for userId={}", userId);
    }
}
