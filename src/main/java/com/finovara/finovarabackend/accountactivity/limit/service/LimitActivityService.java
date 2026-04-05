package com.finovara.finovarabackend.accountactivity.limit.service;

import com.finovara.finovarabackend.accountactivity.limit.dto.LimitActivityDto;
import com.finovara.finovarabackend.accountactivity.limit.mapper.LimitActivityMapper;
import com.finovara.finovarabackend.accountactivity.limit.model.LimitActivity;
import com.finovara.finovarabackend.accountactivity.limit.model.LimitActivitySort;
import com.finovara.finovarabackend.accountactivity.limit.model.LimitActivityType;
import com.finovara.finovarabackend.accountactivity.limit.repository.LimitActivityRepository;
import com.finovara.finovarabackend.limit.model.Limit;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.util.user.service.UserManagerService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LimitActivityService {

    @Value("${user-activity.limit.page-size}")
    private int pageSize;

    private final UserManagerService userManagerService;
    private final LimitActivityRepository limitActivityRepository;
    private final LimitActivityMapper limitActivityMapper;

    @Transactional
    public void createLimitActivity(String email, LimitActivityType limitActivityType, Limit limit) {
        buildLimitActivity(email, limitActivityType, limit);
    }

    public void updateLimitActivity(String email, LimitActivityType limitActivityType, Limit limit, BigDecimal previousAmount) {
        LimitActivity limitActivity = buildLimitActivity(email, limitActivityType, limit);
        limitActivity.setPreviousAmount(previousAmount);
    }

    public List<LimitActivityDto> getLimitActivity(String email, LimitActivitySort sort) {

        Pageable pageable = switch (sort) {
            case NEWEST -> PageRequest.of(0, pageSize, Sort.by("date").descending());
            case OLDEST -> PageRequest.of(0, pageSize, Sort.by("date").ascending());
        };

        return limitActivityRepository.findByUserAssignedEmail(email, pageable)
                .stream().map(limitActivityMapper::mapToLimitActivity)
                .toList();
    }

    @Transactional
    private LimitActivity buildLimitActivity(String email, LimitActivityType limitActivityType, Limit limit) {
        User user = userManagerService.getUserByEmailOrThrow(email);

        LimitActivity limitActivity = LimitActivity.builder()
                .userAssigned(user)
                .limitActivityType(limitActivityType)
                .periodType(limit.getPeriodType())
                .amount(limit.getAmount())
                .date(LocalDateTime.now())
                .build();

        limitActivityRepository.save(limitActivity);
        return limitActivity;
    }
}
