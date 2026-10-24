package com.finovara.financeservice.piggybank.goalplanner.service;

import com.finovara.contracts.exception.conflict.EntityAlreadyExistsException;
import com.finovara.contracts.exception.notfound.RequestedEntityNotFoundException;
import com.finovara.financeservice.piggybank.goalplanner.dto.GoalPlannerCompletionSummaryDto;
import com.finovara.financeservice.piggybank.goalplanner.dto.GoalPlannerDto;
import com.finovara.financeservice.piggybank.goalplanner.dto.GoalPlannerSummaryDto;
import com.finovara.financeservice.piggybank.goalplanner.mapper.GoalPlannerMapper;
import com.finovara.financeservice.piggybank.goalplanner.model.GoalPlanner;
import com.finovara.financeservice.piggybank.goalplanner.repository.GoalPlannerRepository;
import com.finovara.financeservice.piggybank.model.PiggyBank;
import com.finovara.financeservice.piggybank.repository.PiggyBankRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class GoalPlannerService {

    private final GoalPlannerRepository goalPlannerRepository;
    private final PiggyBankRepository piggyBankRepository;
    private final GoalPlannerMapper goalPlannerMapper;

    @Transactional
    public Long createGoalPlanner(Long userId, GoalPlannerDto goalPlannerDto) {
        PiggyBank piggyBank = piggyBankRepository
                .findByIdAndUserIdForUpdate(goalPlannerDto.piggyBankId(), userId)
                .orElseThrow(() -> new RequestedEntityNotFoundException("Piggy bank not found"));

        if (piggyBank.getGoalPlanner() != null) {
            throw new EntityAlreadyExistsException("Goal planner already exists for this piggy bank");
        }

        GoalPlanner goalPlanner = goalPlannerRepository.save(GoalPlanner.builder()
                .topic(goalPlannerDto.topic())
                .targetDate(goalPlannerDto.targetDate())
                .createdAt(LocalDateTime.now())
                .userId(userId)
                .build());

        piggyBank.setGoalPlanner(goalPlanner);
        checkAndMarkGoalCompletion(piggyBank);

        return goalPlanner.getId();
    }

    @Transactional(readOnly = true)
    public GoalPlannerDto getGoalPlanner(Long userId, Long piggyBankId) {
        PiggyBank piggyBank = findPiggyBankWithPlanner(userId, piggyBankId);
        return goalPlannerMapper.toDto(piggyBank.getGoalPlanner(), piggyBank);
    }

    @Transactional(readOnly = true)
    public GoalPlannerSummaryDto getGoalPlannerSummary(Long userId, Long piggyBankId) {
        PiggyBank piggyBank = findPiggyBankWithPlanner(userId, piggyBankId);
        return goalPlannerMapper.toSummaryDto(piggyBank.getGoalPlanner(), piggyBank);
    }

    @Transactional(readOnly = true)
    public GoalPlannerCompletionSummaryDto getGoalCompletionSummary(Long userId, Long piggyBankId) {
        PiggyBank piggyBank = findPiggyBankWithPlanner(userId, piggyBankId);
        GoalPlanner goalPlanner = piggyBank.getGoalPlanner();

        if (goalPlanner.getCompletedAt() == null) {
            throw new RequestedEntityNotFoundException("Goal not yet completed");
        }

        return goalPlannerMapper.toCompletionDto(goalPlanner, piggyBank);
    }

    @Transactional
    public void checkAndMarkGoalCompletion(PiggyBank piggyBank) {
        GoalPlanner goalPlanner = piggyBank.getGoalPlanner();
        if (goalPlanner == null) {
            return;
        }

        BigDecimal currentAmount = piggyBank.getAmount();
        BigDecimal goalAmount = piggyBank.getGoalAmount();
        boolean goalReached = goalAmount != null && currentAmount.compareTo(goalAmount) >= 0;

        if (goalReached && goalPlanner.getCompletedAt() == null) {
            goalPlanner.setCompletedAt(LocalDateTime.now());
        } else if (!goalReached && goalPlanner.getCompletedAt() != null) {
            goalPlanner.setCompletedAt(null);
        }
    }

    private PiggyBank findPiggyBankWithPlanner(Long userId, Long piggyBankId) {
        PiggyBank piggyBank = piggyBankRepository.findByIdAndUserId(piggyBankId, userId)
                .orElseThrow(() -> new RequestedEntityNotFoundException("Piggy bank not found"));

        if (piggyBank.getGoalPlanner() == null) {
            throw new RequestedEntityNotFoundException("Goal planner not found");
        }
        return piggyBank;
    }
}