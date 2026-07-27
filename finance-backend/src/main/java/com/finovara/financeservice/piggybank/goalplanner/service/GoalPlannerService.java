package com.finovara.financeservice.piggybank.goalplanner.service;

import com.finovara.contracts.exception.notfound.RequestedEntityNotFoundException;
import com.finovara.financeservice.piggybank.goalplanner.dto.GoalPlannerCompletionSummaryDto;
import com.finovara.financeservice.piggybank.goalplanner.dto.GoalPlannerDto;
import com.finovara.financeservice.piggybank.goalplanner.dto.GoalPlannerSummaryDto;
import com.finovara.financeservice.piggybank.goalplanner.mapper.GoalPlannerMapper;
import com.finovara.financeservice.piggybank.goalplanner.model.GoalPlanner;
import com.finovara.financeservice.piggybank.goalplanner.repository.GoalPlannerRepository;
import com.finovara.financeservice.piggybank.model.PiggyBank;
import com.finovara.financeservice.util.piggybank.manager.PiggyBankManagerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class GoalPlannerService {

    private final GoalPlannerRepository goalPlannerRepository;
    private final PiggyBankManagerService piggyBankManagerService;
    private final GoalPlannerMapper goalPlannerMapper;

    @Transactional
    public Long createGoalPlanner(Long userId, GoalPlannerDto goalPlannerDto) {
        PiggyBank piggyBank = piggyBankManagerService.getPiggyBankByUserId(goalPlannerDto.piggyBankId(), userId);

        GoalPlanner goalPlanner = GoalPlanner.builder()
                .topic(goalPlannerDto.topic())
                .targetDate(goalPlannerDto.targetDate())
                .createdAt(LocalDateTime.now())
                .userId(userId)
                .piggyBankAssigned(piggyBank)
                .build();

        return goalPlannerRepository.save(goalPlanner).getId();
    }

    public GoalPlannerDto getGoalPlanner(Long userId, Long piggyBankId) {
        GoalPlanner goalPlanner = findGoalPlanner(userId, piggyBankId);
        return goalPlannerMapper.toDto(goalPlanner);
    }

    public GoalPlannerSummaryDto getGoalPlannerSummary(Long userId, Long piggyBankId) {
        GoalPlanner goalPlanner = findGoalPlanner(userId, piggyBankId);
        return goalPlannerMapper.toSummaryDto(goalPlanner);
    }

    public GoalPlannerCompletionSummaryDto getGoalCompletionSummary(Long userId, Long piggyBankId) {
        GoalPlanner goalPlanner = findGoalPlanner(userId, piggyBankId);

        if (goalPlanner.getCompletedAt() == null) {
            throw new RequestedEntityNotFoundException("Goal not yet completed");
        }

        return goalPlannerMapper.toCompletionDto(goalPlanner);
    }

    @Transactional
    public void checkAndMarkGoalCompletion(GoalPlanner goalPlanner) {
        if (goalPlanner == null) {
            return;
        }

        BigDecimal currentAmount = goalPlanner.getPiggyBankAssigned().getAmount();
        BigDecimal goalAmount = goalPlanner.getPiggyBankAssigned().getGoalAmount();
        boolean goalReached = currentAmount.compareTo(goalAmount) >= 0;

        if (goalReached && goalPlanner.getCompletedAt() == null) {
            goalPlanner.setCompletedAt(LocalDateTime.now());
            goalPlannerRepository.save(goalPlanner);
        } else if (!goalReached && goalPlanner.getCompletedAt() != null) {
            goalPlanner.setCompletedAt(null);
            goalPlannerRepository.save(goalPlanner);
        }
    }

    private GoalPlanner findGoalPlanner(Long userId, Long piggyBankId) {
        return goalPlannerRepository.findByPiggyBankIdAndUserId(piggyBankId, userId)
                .orElseThrow(() -> new RequestedEntityNotFoundException("Goal planner not found"));
    }
}