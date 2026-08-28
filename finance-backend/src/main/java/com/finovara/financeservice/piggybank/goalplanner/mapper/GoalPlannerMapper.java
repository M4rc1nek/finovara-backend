package com.finovara.financeservice.piggybank.goalplanner.mapper;

import com.finovara.financeservice.piggybank.goalplanner.dto.GoalPlannerCompletionSummaryDto;
import com.finovara.financeservice.piggybank.goalplanner.dto.GoalPlannerDto;
import com.finovara.financeservice.piggybank.goalplanner.dto.GoalPlannerSummaryDto;
import com.finovara.financeservice.piggybank.goalplanner.model.GoalPlanner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import static com.finovara.financeservice.util.transaction.piggybank.goalplanner.calculator.GoalPlannerCalculator.*;

@Component
public class GoalPlannerMapper {

    public GoalPlannerDto toDto(GoalPlanner goalPlanner) {
        return new GoalPlannerDto(
                goalPlanner.getId(),
                goalPlanner.getPiggyBankAssigned().getId(),
                goalPlanner.getTopic(),
                goalPlanner.getPiggyBankAssigned().getGoalAmount(),
                goalPlanner.getTargetDate(),
                goalPlanner.getCreatedAt()
        );
    }

    public GoalPlannerSummaryDto toSummaryDto(GoalPlanner goalPlanner) {
        long daysUntilTarget = ChronoUnit.DAYS.between(LocalDate.now(), goalPlanner.getTargetDate());
        return new GoalPlannerSummaryDto(
                calculateDailyInstallment(goalPlanner),
                calculateWeeklyInstallment(goalPlanner),
                calculateMonthlyInstallment(goalPlanner),
                daysUntilTarget
        );
    }

    public GoalPlannerCompletionSummaryDto toCompletionDto(GoalPlanner goalPlanner) {
        long durationDays = ChronoUnit.DAYS.between(goalPlanner.getCreatedAt(), goalPlanner.getCompletedAt());
        long durationHours = ChronoUnit.HOURS.between(goalPlanner.getCreatedAt(), goalPlanner.getCompletedAt());
        long durationMinutes = ChronoUnit.MINUTES.between(goalPlanner.getCreatedAt(), goalPlanner.getCompletedAt());

        return new GoalPlannerCompletionSummaryDto(
                durationDays,
                durationHours,
                durationMinutes,
                goalPlanner.getPiggyBankAssigned().getAmount(),
                goalPlanner.getCompletedAt()
        );
    }
}