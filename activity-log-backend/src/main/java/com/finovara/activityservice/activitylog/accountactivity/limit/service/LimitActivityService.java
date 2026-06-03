package com.finovara.activityservice.activitylog.accountactivity.limit.service;

import com.finovara.activityservice.activitylog.accountactivity.core.AccountActivityCore;
import com.finovara.activityservice.activitylog.accountactivity.limit.dto.LimitActivityDto;
import com.finovara.activityservice.activitylog.accountactivity.limit.mapper.LimitActivityMapper;
import com.finovara.activityservice.activitylog.accountactivity.limit.model.LimitActivity;
import com.finovara.activityservice.activitylog.accountactivity.limit.repository.LimitActivityRepository;
import com.finovara.contracts.event.activity.limit.LimitActivityEvent;
import com.finovara.contracts.model.PeriodType;
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
public class LimitActivityService extends AccountActivityCore<LimitActivity, LimitActivityDto> {

    @Value("${user-activity.limit.page-size}")
    private int pageSize;

    private final LimitActivityRepository limitActivityRepository;
    private final LimitActivityMapper limitActivityMapper;

    @Transactional
    public void handleEvent(LimitActivityEvent event) {
        LimitActivity limitActivity = LimitActivity.builder()
                .userId(event.userId())
                .limitActivityType(event.type())
                .periodType(mapPeriodType(event.periodType()))
                .amount(event.amount())
                .previousAmount(event.previousAmount())
                .createdAt(event.occurredAt())
                .build();

        limitActivityRepository.save(limitActivity);
        log.info("Created activity type: {}, userId: {}", event.type(), event.userId());
    }

    public List<LimitActivityDto> getLimitActivity(Long userId, SortType sort) {
        return getActivities(userId, sort, pageSize);
    }

    @Override
    protected List<LimitActivity> getRepositoryFindByUserId(Long userId, Pageable pageable) {
        return limitActivityRepository.findByUserId(userId, pageable);
    }

    @Override
    protected LimitActivityDto mapToDto(LimitActivity entity) {
        return limitActivityMapper.mapToLimitActivity(entity);
    }

    private PeriodType mapPeriodType(String periodType) {
        return periodType == null ? null : PeriodType.valueOf(periodType);
    }
}
