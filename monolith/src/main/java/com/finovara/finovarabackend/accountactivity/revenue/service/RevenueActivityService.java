package com.finovara.finovarabackend.accountactivity.revenue.service;

import com.finovara.finovarabackend.accountactivity.core.AccountActivityCore;
import com.finovara.finovarabackend.util.model.SortType;
import com.finovara.finovarabackend.accountactivity.revenue.dto.RevenueActivityDto;
import com.finovara.finovarabackend.accountactivity.revenue.mapper.RevenueActivityMapper;
import com.finovara.finovarabackend.accountactivity.revenue.model.RevenueActivity;
import com.finovara.finovarabackend.accountactivity.revenue.model.RevenueActivityType;
import com.finovara.finovarabackend.accountactivity.revenue.repository.RevenueActivityRepository;
import com.finovara.finovarabackend.revenue.model.Revenue;
import com.finovara.finovarabackend.revenue.model.RevenueCategory;
import com.finovara.finovarabackend.util.user.service.UserManagerService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class RevenueActivityService extends AccountActivityCore<RevenueActivity, RevenueActivityDto, Revenue> {

    @Value("${user-activity.revenue.page-size}")
    private int pageSize;

    private final RevenueActivityRepository revenueActivityRepository;
    private final RevenueActivityMapper revenueActivityMapper;

    public RevenueActivityService(UserManagerService userManagerService,
                                  RevenueActivityRepository revenueActivityRepository,
                                  RevenueActivityMapper revenueActivityMapper) {
        super(userManagerService);
        this.revenueActivityRepository = revenueActivityRepository;
        this.revenueActivityMapper = revenueActivityMapper;
    }

    @Transactional
    public void createRevenueActivity(Long userId, RevenueActivityType revenueActivityType, Revenue revenue) {
        RevenueActivity revenueActivity = buildActivity(userId, revenue);
        revenueActivity.setType(revenueActivityType);
        revenueActivityRepository.save(revenueActivity);
    }

    @Transactional
    public void updateRevenueActivity(Long userId, RevenueActivityType revenueActivityType, Revenue revenue, BigDecimal previousAmount, RevenueCategory previousCategory) {
        RevenueActivity revenueActivity = buildActivity(userId, revenue);
        revenueActivity.setType(revenueActivityType);
        revenueActivity.setPreviousCategory(previousCategory);
        revenueActivity.setPreviousAmount(previousAmount);
        revenueActivityRepository.save(revenueActivity);
    }

    public List<RevenueActivityDto> getRevenueActivity(Long userId, SortType sort) {
        return getActivities(userId, sort, pageSize);
    }

    @Override
    protected List<RevenueActivity> getRepositoryFindByUserId(Long userId, Pageable pageable) {
        return revenueActivityRepository.findByUserAssignedId(userId, pageable);
    }

    @Override
    protected RevenueActivityDto mapToDto(RevenueActivity entity) {
        return revenueActivityMapper.mapToRevenueActivity(entity);
    }

    @Override
    protected RevenueActivity buildActivity(Long userId, Revenue revenue) {
        return RevenueActivity.builder()
                .userAssigned(getUser(userId))
                .amount(revenue.getAmount())
                .category(revenue.getCategory())
                .createdAt(LocalDateTime.now())
                .build();
    }
}