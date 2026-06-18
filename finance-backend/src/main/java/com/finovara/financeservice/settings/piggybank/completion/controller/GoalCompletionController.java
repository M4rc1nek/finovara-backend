package com.finovara.financeservice.settings.piggybank.completion.controller;

import com.finovara.financeservice.security.SecurityUtils;
import com.finovara.financeservice.settings.piggybank.completion.dto.GoalCompletionDto;
import com.finovara.financeservice.settings.piggybank.completion.service.GoalCompletionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/piggybank-settings/goal-completion")
@RequiredArgsConstructor
public class GoalCompletionController {

    private final GoalCompletionService goalCompletionService;

    @PatchMapping("/{piggyBankId}")
    public ResponseEntity<Void> setGoalCompletion(@RequestBody @Valid GoalCompletionDto goalCompletionDto, @PathVariable Long piggyBankId) {
        goalCompletionService.setGoalCompletion(SecurityUtils.getCurrentUserId(), piggyBankId, goalCompletionDto);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{piggyBankId}")
    public ResponseEntity<Void> saveGoalCompletion(@PathVariable Long piggyBankId, @RequestBody GoalCompletionDto goalCompletionDto) {
        goalCompletionService.saveGoalCompletion(piggyBankId, SecurityUtils.getCurrentUserId(), goalCompletionDto);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{piggyBankId}")
    public ResponseEntity<GoalCompletionDto> getCompletionDto(@PathVariable Long piggyBankId) {
        return ResponseEntity.ok(goalCompletionService.getCompletionDto(SecurityUtils.getCurrentUserId(), piggyBankId));
    }

}
