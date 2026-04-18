package com.finovara.finovarabackend.accountactivity.limit.service;

import com.finovara.finovarabackend.accountactivity.core.AccountActivityCore;
import com.finovara.finovarabackend.accountactivity.limit.dto.LimitActivityDto;
import com.finovara.finovarabackend.accountactivity.limit.mapper.LimitActivityMapper;
import com.finovara.finovarabackend.accountactivity.limit.model.LimitActivity;
import com.finovara.finovarabackend.accountactivity.limit.model.LimitActivityType;
import com.finovara.finovarabackend.accountactivity.limit.repository.LimitActivityRepository;
import com.finovara.finovarabackend.util.model.SortType;
import com.finovara.finovarabackend.limit.model.Limit;
import com.finovara.finovarabackend.util.user.service.UserManagerService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class LimitActivityService extends AccountActivityCore<LimitActivity, LimitActivityDto, Limit> {

    @Value("${user-activity.limit.page-size}")
    private int pageSize;

    private final LimitActivityRepository limitActivityRepository;
    private final LimitActivityMapper limitActivityMapper;

    public LimitActivityService(UserManagerService userManagerService,
                                LimitActivityRepository limitActivityRepository,
                                LimitActivityMapper limitActivityMapper) {
        super(userManagerService);
        this.limitActivityRepository = limitActivityRepository;
        this.limitActivityMapper = limitActivityMapper;
    }

    @Transactional
    public void createLimitActivity(Long userId, LimitActivityType limitActivityType, Limit limit) {
        LimitActivity limitActivity = buildActivity(userId, limit);
        limitActivity.setLimitActivityType(limitActivityType);
        limitActivityRepository.save(limitActivity);
    }

    @Transactional
    public void updateLimitActivity(Long userId, LimitActivityType limitActivityType, Limit limit, BigDecimal previousAmount) {
        LimitActivity limitActivity = buildActivity(userId, limit);
        limitActivity.setLimitActivityType(limitActivityType);
        limitActivity.setPreviousAmount(previousAmount);
        limitActivityRepository.save(limitActivity);
    }

    public List<LimitActivityDto> getLimitActivity(Long userId, SortType sort) {
        return getActivities(userId, sort, pageSize);
    }

    @Override
    protected List<LimitActivity> getRepositoryFindByUserId(Long userId, Pageable pageable) {
        return limitActivityRepository.findByUserAssignedId(userId, pageable);
    }

    @Override
    protected LimitActivityDto mapToDto(LimitActivity entity) {
        return limitActivityMapper.mapToLimitActivity(entity);
    }

    @Override
    protected LimitActivity buildActivity(Long userId, Limit limit) {
        return LimitActivity.builder()
                .userAssigned(getUser(userId))
                .periodType(limit.getPeriodType())
                .amount(limit.getAmount())
                .createdAt(LocalDateTime.now())
                .build();
    }
}
