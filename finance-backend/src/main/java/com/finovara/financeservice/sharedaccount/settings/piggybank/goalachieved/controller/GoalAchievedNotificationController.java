package com.finovara.financeservice.sharedaccount.settings.piggybank.goalachieved.controller;

import com.finovara.financeservice.security.SecurityUtils;
import com.finovara.financeservice.sharedaccount.settings.piggybank.goalachieved.dto.GoalAchievedNotificationDto;
import com.finovara.financeservice.sharedaccount.settings.piggybank.goalachieved.service.GoalAchievedNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/shared-accounts/settings/piggy-bank-goal-achieved-notification")
@RequiredArgsConstructor
public class GoalAchievedNotificationController {

    private final GoalAchievedNotificationService goalAchievedNotificationService;

    @PatchMapping
    public ResponseEntity<Void> saveGoalAchievedNotification(@RequestBody GoalAchievedNotificationDto goalAchievedNotificationDto) {
        goalAchievedNotificationService.saveGoalAchievedNotification(SecurityUtils.getCurrentUserId(), goalAchievedNotificationDto);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<GoalAchievedNotificationDto> getGoalAchievedNotification() {
        return ResponseEntity.ok(goalAchievedNotificationService.getGoalAchievedNotification(SecurityUtils.getCurrentUserId()));
    }
}
