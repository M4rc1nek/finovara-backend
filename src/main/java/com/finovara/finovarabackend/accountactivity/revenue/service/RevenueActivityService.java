package com.finovara.finovarabackend.accountactivity.revenue.service;

import com.finovara.finovarabackend.accountactivity.revenue.dto.RevenueActivityDto;
import com.finovara.finovarabackend.accountactivity.revenue.mapper.RevenueActivityMapper;
import com.finovara.finovarabackend.accountactivity.revenue.model.RevenueActivitySort;
import com.finovara.finovarabackend.accountactivity.revenue.model.RevenueActivityType;
import com.finovara.finovarabackend.accountactivity.revenue.model.RevenueActivity;
import com.finovara.finovarabackend.accountactivity.revenue.repository.RevenueActivityRepository;
import com.finovara.finovarabackend.revenue.model.Revenue;
import com.finovara.finovarabackend.revenue.model.RevenueCategory;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.util.service.user.service.UserManagerService;
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
public class RevenueActivityService {

    @Value("${user-activity.revenue.page-size}")
    private int pageSize;

    private final UserManagerService userManagerService;
    private final RevenueActivityRepository revenueActivityRepository;
    private final RevenueActivityMapper revenueActivityMapper;

    @Transactional
    public void createRevenueActivity(String email, RevenueActivityType revenueActivityType, Revenue revenue) {
        buildRevenueActivity(email, revenueActivityType, revenue);
    }

    @Transactional
    public void updateRevenueActivity(String email, RevenueActivityType revenueActivityType, Revenue revenue, BigDecimal previousAmount, RevenueCategory previousCategory) {
        RevenueActivity revenueActivity = buildRevenueActivity(email, revenueActivityType, revenue);
        revenueActivity.setPreviousCategory(previousCategory);
        revenueActivity.setPreviousAmount(previousAmount);
    }

    public List<RevenueActivityDto> getRevenueActivity(String email, RevenueActivitySort sort) {

        Pageable pageable = switch (sort) {
            case NEWEST -> PageRequest.of(0, pageSize, Sort.by("date").descending());
            case OLDEST -> PageRequest.of(0, pageSize, Sort.by("date").ascending());
            case AMOUNT_DESC -> PageRequest.of(0, pageSize, Sort.by("amount").descending());
            case AMOUNT_ASC -> PageRequest.of(0, pageSize, Sort.by("amount").ascending());
        };

        return revenueActivityRepository.findByUserAssignedEmail(email, pageable)
                .stream().map(revenueActivityMapper::mapToRevenueActivity)
                .toList();
    }

    @Transactional
    private RevenueActivity buildRevenueActivity(String email, RevenueActivityType revenueActivityType, Revenue revenue) {
        User user = userManagerService.getUserByEmailOrThrow(email);

        RevenueActivity revenueActivity = RevenueActivity.builder()
                .userAssigned(user)
                .type(revenueActivityType)
                .amount(revenue.getAmount())
                .category(revenue.getCategory())
                .date(LocalDateTime.now())
                .build();
        revenueActivityRepository.save(revenueActivity);
        return revenueActivity;
    }

}