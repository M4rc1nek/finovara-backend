package com.finovara.financeservice.piggybank.goalplanner.mapper;

import com.finovara.financeservice.piggybank.goalplanner.dto.GoalPlannerCompletionSummaryDto;
import com.finovara.financeservice.piggybank.goalplanner.dto.GoalPlannerDto;
import com.finovara.financeservice.piggybank.goalplanner.dto.GoalPlannerSummaryDto;
import com.finovara.financeservice.piggybank.goalplanner.model.GoalPlanner;
import com.finovara.financeservice.piggybank.model.PiggyBank;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import static com.finovara.financeservice.util.transaction.piggybank.goalplanner.calculator.GoalPlannerCalculator.*;

@Component
public class GoalPlannerMapper {

    public GoalPlannerDto toDto(GoalPlanner goalPlanner, PiggyBank piggyBank) {
        return new GoalPlannerDto(
                goalPlanner.getId(),
                piggyBank.getId(),
                goalPlanner.getTopic(),
                piggyBank.getGoalAmount(),
                goalPlanner.getTargetDate(),
                goalPlanner.getCreatedAt()
        );
    }

    public GoalPlannerSummaryDto toSummaryDto(GoalPlanner goalPlanner, PiggyBank piggyBank) {
        long daysUntilTarget = ChronoUnit.DAYS.between(LocalDate.now(), goalPlanner.getTargetDate());
        return new GoalPlannerSummaryDto(
                calculateDailyInstallment(goalPlanner, piggyBank),
                calculateWeeklyInstallment(goalPlanner, piggyBank),
                calculateMonthlyInstallment(goalPlanner, piggyBank),
                daysUntilTarget
        );
    }

    public GoalPlannerCompletionSummaryDto toCompletionDto(GoalPlanner goalPlanner, PiggyBank piggyBank) {
        LocalDateTime from = goalPlanner.getCreatedAt();
        LocalDateTime to = goalPlanner.getCompletedAt();

        return new GoalPlannerCompletionSummaryDto(
                ChronoUnit.DAYS.between(from, to),
                ChronoUnit.HOURS.between(from, to),
                ChronoUnit.MINUTES.between(from, to),
                piggyBank.getAmount(),
                to
        );
    }
}