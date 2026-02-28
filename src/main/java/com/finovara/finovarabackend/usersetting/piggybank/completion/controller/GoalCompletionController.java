package com.finovara.finovarabackend.usersetting.piggybank.completion.controller;

import com.finovara.finovarabackend.security.SecurityUtils;
import com.finovara.finovarabackend.usersetting.piggybank.completion.dto.GoalCompletionDto;
import com.finovara.finovarabackend.usersetting.piggybank.completion.service.GoalCompletionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/piggybank-settings/goal-completion")
@RequiredArgsConstructor
public class GoalCompletionController {

    private final GoalCompletionService goalCompletionService;

    @PutMapping("/{piggyBankId}")
    public ResponseEntity<Void> addGoalCompletion(@PathVariable Long piggyBankId, @RequestBody GoalCompletionDto goalCompletionDto) {
        goalCompletionService.addGoalCompletion(piggyBankId, SecurityUtils.getCurrentUserEmail(), goalCompletionDto);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{piggyBankId}")
    public ResponseEntity<GoalCompletionDto> getCompletionDto(@PathVariable Long piggyBankId) {
        return ResponseEntity.ok(goalCompletionService.getCompletionDto(SecurityUtils.getCurrentUserEmail(), piggyBankId));
    }

    @PatchMapping("/{piggyBankId}")
    public ResponseEntity<Void> saveGoalCompletion(@RequestBody @Valid GoalCompletionDto goalCompletionDto, @PathVariable Long piggyBankId) {
        goalCompletionService.saveGoalCompletion(SecurityUtils.getCurrentUserEmail(), piggyBankId, goalCompletionDto);
        return ResponseEntity.noContent().build();
    }

}



