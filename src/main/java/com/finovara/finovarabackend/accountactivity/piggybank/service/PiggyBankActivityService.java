package com.finovara.finovarabackend.accountactivity.piggybank.service;

import com.finovara.finovarabackend.accountactivity.core.AccountActivityCore;
import com.finovara.finovarabackend.util.model.SortType;
import com.finovara.finovarabackend.accountactivity.piggybank.dto.PiggyBankActivityDto;
import com.finovara.finovarabackend.accountactivity.piggybank.mapper.PiggyBankActivityMapper;
import com.finovara.finovarabackend.accountactivity.piggybank.model.PiggyBankActivity;
import com.finovara.finovarabackend.accountactivity.piggybank.model.PiggyBankActivityType;
import com.finovara.finovarabackend.accountactivity.piggybank.repository.PiggyBankActivityRepository;
import com.finovara.finovarabackend.piggybank.model.PiggyBank;
import com.finovara.finovarabackend.piggybank.model.PiggyBankGoalType;
import com.finovara.finovarabackend.util.user.service.UserManagerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class PiggyBankActivityService extends AccountActivityCore<PiggyBankActivity, PiggyBankActivityDto, PiggyBank> {

    @Value("${user-activity.piggy-bank.page-size}")
    private int pageSize;

    private final PiggyBankActivityRepository piggyBankActivityRepository;
    private final PiggyBankActivityMapper piggyBankActivityMapper;

    public PiggyBankActivityService(UserManagerService userManagerService,
                                    PiggyBankActivityRepository piggyBankActivityRepository,
                                    PiggyBankActivityMapper piggyBankActivityMapper) {
        super(userManagerService);
        this.piggyBankActivityRepository = piggyBankActivityRepository;
        this.piggyBankActivityMapper = piggyBankActivityMapper;
    }

    @Transactional
    public void createSimplePiggyBankActivity(String email, PiggyBank piggyBank, PiggyBankActivityType activityType) {
        PiggyBankActivity piggyBankActivity = buildActivity(email, piggyBank);
        piggyBankActivity.setActivityType(activityType);
        piggyBankActivityRepository.save(piggyBankActivity);
    }

    @Transactional
    public void createPaymentPiggyBankActivity(String email, PiggyBank piggyBank, PiggyBankActivityType activityType, BigDecimal paidAmount) {
        PiggyBankActivity piggyBankActivity = buildActivity(email, piggyBank);
        piggyBankActivity.setActivityType(activityType);
        piggyBankActivity.setAmountPaid(paidAmount);
        piggyBankActivityRepository.save(piggyBankActivity);
    }

    @Transactional
    public void createEditPiggyBankActivity(String email, PiggyBank piggyBank, PiggyBankActivityType activityType,
                                            BigDecimal previousGoalAmount, PiggyBankGoalType previousGoalType, String previousPiggyBankName) {
        PiggyBankActivity piggyBankActivity = buildActivity(email, piggyBank);
        piggyBankActivity.setActivityType(activityType);
        piggyBankActivity.setPreviousPiggyBankName(previousPiggyBankName);
        piggyBankActivity.setPreviousGoalType(previousGoalType);
        piggyBankActivity.setPreviousGoalAmount(previousGoalAmount);
        piggyBankActivityRepository.save(piggyBankActivity);
    }

    public List<PiggyBankActivityDto> getPiggyBankActivities(String email, SortType sort) {
        return getActivities(email, sort, pageSize);
    }

    @Override
    protected List<PiggyBankActivity> getRepositoryFindByUserEmail(String email, Pageable pageable) {
        return piggyBankActivityRepository.findByUserAssignedEmail(email, pageable);
    }

    @Override
    protected PiggyBankActivityDto mapToDto(PiggyBankActivity entity) {
        return piggyBankActivityMapper.mapToPiggyBankActivity(entity);
    }

    @Override
    protected PiggyBankActivity buildActivity(String email, PiggyBank piggyBank) {
        return PiggyBankActivity.builder()
                .userAssigned(getUser(email))
                .piggyBankName(piggyBank.getName())
                .goalType(piggyBank.getGoalType())
                .goalAmount(piggyBank.getGoalAmount())
                .createdAt(LocalDateTime.now())
                .build();
    }
}
