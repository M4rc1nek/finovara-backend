package com.finovara.finovarabackend.accountactivity.piggybank.service;

import com.finovara.finovarabackend.accountactivity.piggybank.dto.PiggyBankActivityDto;
import com.finovara.finovarabackend.accountactivity.piggybank.mapper.PiggyBankActivityMapper;
import com.finovara.finovarabackend.accountactivity.piggybank.model.PiggyBankActivity;
import com.finovara.finovarabackend.accountactivity.piggybank.model.PiggyBankActivitySort;
import com.finovara.finovarabackend.accountactivity.piggybank.model.PiggyBankActivityType;
import com.finovara.finovarabackend.accountactivity.piggybank.repository.PiggyBankActivityRepository;
import com.finovara.finovarabackend.piggybank.model.PiggyBank;
import com.finovara.finovarabackend.piggybank.model.PiggyBankGoalType;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.util.user.service.UserManagerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PiggyBankActivityService {

    @Value("${user-activity.piggy-bank.page-size}")
    private int pageSize;

    private final UserManagerService userManagerService;
    private final PiggyBankActivityRepository piggyBankActivityRepository;
    private final PiggyBankActivityMapper piggyBankActivityMapper;

    @Transactional
    public void createSimplePiggyBankActivity(String email, PiggyBank piggyBank, PiggyBankActivityType activityType) {
        buildPiggyBankActivity(email, piggyBank, activityType);
    }

    @Transactional
    public void createPaymentPiggyBankActivity(String email, PiggyBank piggyBank, PiggyBankActivityType activityType, BigDecimal paidAmount) {
        PiggyBankActivity piggyBankActivity = buildPiggyBankActivity(email, piggyBank, activityType);
        piggyBankActivity.setAmountPaid(paidAmount);
    }

    @Transactional
    public void createEditPiggyBankActivity(String email, PiggyBank piggyBank, PiggyBankActivityType activityType,
                                            BigDecimal previousGoalAmount, PiggyBankGoalType previousGoalType, String previousPiggyBankName) {
        PiggyBankActivity piggyBankActivity = buildPiggyBankActivity(email, piggyBank, activityType);
        piggyBankActivity.setPreviousPiggyBankName(previousPiggyBankName);
        piggyBankActivity.setPreviousGoalType(previousGoalType);
        piggyBankActivity.setPreviousGoalAmount(previousGoalAmount);
    }

    public List<PiggyBankActivityDto> getPiggyBankActivities(String email, PiggyBankActivitySort sort) {

        Pageable pageable = switch (sort) {
            case NEWEST -> PageRequest.of(0, pageSize, Sort.by("createdAt").descending());
            case OLDEST -> PageRequest.of(0, pageSize, Sort.by("createdAt").ascending());
        };

        return piggyBankActivityRepository.findByUserAssignedEmail(email, pageable)
                .stream().map(piggyBankActivityMapper::mapToPiggyBankActivity)
                .toList();
    }

    private PiggyBankActivity buildPiggyBankActivity(String email, PiggyBank piggyBank, PiggyBankActivityType activityType) {
        User user = userManagerService.getUserByEmailOrThrow(email);

        PiggyBankActivity piggyBankActivity = PiggyBankActivity.builder()
                .userAssigned(user)
                .piggyBankName(piggyBank.getName())
                .activityType(activityType)
                .goalType(piggyBank.getGoalType())
                .goalAmount(piggyBank.getGoalAmount())
                .date(LocalDateTime.now())
                .build();

        piggyBankActivityRepository.save(piggyBankActivity);
        return piggyBankActivity;
    }
}
