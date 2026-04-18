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
    public void createLimitActivity(String email, LimitActivityType limitActivityType, Limit limit) {
        LimitActivity limitActivity = buildActivity(email, limit);
        limitActivity.setLimitActivityType(limitActivityType);
        limitActivityRepository.save(limitActivity);
    }

    public void updateLimitActivity(String email, LimitActivityType limitActivityType, Limit limit, BigDecimal previousAmount) {
        LimitActivity limitActivity = buildActivity(email, limit);
        limitActivity.setLimitActivityType(limitActivityType);
        limitActivity.setPreviousAmount(previousAmount);
        limitActivityRepository.save(limitActivity);
    }

    public List<LimitActivityDto> getLimitActivity(String email, SortType sort) {
        return getActivities(email, sort, pageSize);
    }

    @Override
    protected List<LimitActivity> getRepositoryFindByUserEmail(String email, Pageable pageable) {
        return limitActivityRepository.findByUserAssignedEmail(email, pageable);
    }

    @Override
    protected LimitActivityDto mapToDto(LimitActivity entity) {
        return limitActivityMapper.mapToLimitActivity(entity);
    }

    @Override
    protected LimitActivity buildActivity(String email, Limit limit) {
        return LimitActivity.builder()
                .userAssigned(getUser(email))
                .periodType(limit.getPeriodType())
                .amount(limit.getAmount())
                .createdAt(LocalDateTime.now())
                .build();
    }
}
