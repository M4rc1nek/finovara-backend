package com.finovara.activityservice.activity_log.accountactivity.revenue.service;

import com.finovara.activityservice.activity_log.accountactivity.core.AccountActivityCore;
import com.finovara.activityservice.activity_log.accountactivity.revenue.dto.RevenueActivityDto;
import com.finovara.activityservice.activity_log.accountactivity.revenue.mapper.RevenueActivityMapper;
import com.finovara.activityservice.activity_log.accountactivity.revenue.model.RevenueActivity;
import com.finovara.activityservice.activity_log.accountactivity.revenue.repository.RevenueActivityRepository;
import com.finovara.contracts.event.revenue.RevenueActivityEvent;
import com.finovara.contracts.model.SortType;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RevenueActivityService extends AccountActivityCore<RevenueActivity, RevenueActivityDto> {

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
}
