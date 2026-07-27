package com.finovara.financeservice.piggybank.goalplanner.controller;

import com.finovara.financeservice.piggybank.goalplanner.dto.GoalPlannerCompletionSummaryDto;
import com.finovara.financeservice.piggybank.goalplanner.dto.GoalPlannerDto;
import com.finovara.financeservice.piggybank.goalplanner.dto.GoalPlannerSummaryDto;
import com.finovara.financeservice.piggybank.goalplanner.service.GoalPlannerService;
import com.finovara.financeservice.security.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/piggy-banks/goal-planner")
@RequiredArgsConstructor
public class GoalPlannerController {
    private final GoalPlannerService goalPlannerService;

    @PostMapping
    public ResponseEntity<Long> createGoalPlanner(@Valid @RequestBody GoalPlannerDto goalPlannerDto) {
        return ResponseEntity.ok(goalPlannerService.createGoalPlanner(SecurityUtils.getCurrentUserId(), goalPlannerDto));
    }

    @GetMapping("/{piggyBankId}")
    public ResponseEntity<GoalPlannerDto> getGoalPlanner(@PathVariable Long piggyBankId) {
        return ResponseEntity.ok(goalPlannerService.getGoalPlanner(SecurityUtils.getCurrentUserId(), piggyBankId));
    }

    @GetMapping("/{piggyBankId}/summary")
    public ResponseEntity<GoalPlannerSummaryDto> getGoalPlannerSummary(@PathVariable Long piggyBankId) {
        return ResponseEntity.ok(goalPlannerService.getGoalPlannerSummary(SecurityUtils.getCurrentUserId(), piggyBankId));
    }

    @GetMapping("/{piggyBankId}/completion")
    public ResponseEntity<GoalPlannerCompletionSummaryDto> getGoalCompletionSummary(@PathVariable Long piggyBankId) {
        return ResponseEntity.ok(goalPlannerService.getGoalCompletionSummary(SecurityUtils.getCurrentUserId(), piggyBankId));
    }
}