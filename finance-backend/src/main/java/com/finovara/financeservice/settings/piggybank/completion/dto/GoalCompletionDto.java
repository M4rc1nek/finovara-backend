package com.finovara.financeservice.settings.piggybank.completion.dto;

import com.finovara.financeservice.settings.piggybank.completion.model.GoalCompletionStrategy;

public record GoalCompletionDto(
        GoalCompletionStrategy strategy,
        String authorizationCode

) {
}
