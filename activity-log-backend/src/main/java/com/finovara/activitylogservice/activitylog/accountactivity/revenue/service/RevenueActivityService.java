package com.finovara.activityservice.activitylog.accountactivity.revenue.service;

import com.finovara.activityservice.activitylog.accountactivity.core.AccountActivityCore;
import com.finovara.activityservice.activitylog.accountactivity.revenue.dto.RevenueActivityDto;
import com.finovara.activityservice.activitylog.accountactivity.revenue.mapper.RevenueActivityMapper;
import com.finovara.activityservice.activitylog.accountactivity.revenue.model.RevenueActivity;
import com.finovara.activityservice.activitylog.accountactivity.revenue.repository.RevenueActivityRepository;
import com.finovara.activityservice.activitylog.datadeletable.UserDataDeletable;
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
public class RevenueActivityService extends AccountActivityCore<RevenueActivity, RevenueActivityDto> implements UserDataDeletable {

    @Value("${user-activity.revenue.page-size}")
    private int pageSize;

    private final RevenueActivityRepository revenueActivityRepository;
    private final RevenueActivityMapper revenueActivityMapper;

    @Transactional
    public void handleEvent(RevenueActivityEvent event) {
        RevenueActivity revenueActivity = RevenueActivity.builder()
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

    public List<RevenueActivityDto> getRevenueActivity(Long userId, SortType sort) {
        return getActivities(userId, sort, pageSize);
    }

    @Override
    protected List<RevenueActivity> getRepositoryFindByUserId(Long userId, Pageable pageable) {
        return revenueActivityRepository.findByUserId(userId, pageable);
    }

    @Override
    protected RevenueActivityDto mapToDto(RevenueActivity entity) {
        return revenueActivityMapper.mapToRevenueActivity(entity);
    }

    @Override
    @Transactional
    public void deleteByUserId(Long userId) {
        revenueActivityRepository.deleteByUserId(userId);
        log.info("Deleted revenue activity for userId={}", userId);
    }
}
