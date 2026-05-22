package com.finovara.finovarabackend.usersetting.piggybank.completion.dto;

import com.finovara.finovarabackend.usersetting.piggybank.completion.model.GoalCompletionStrategy;

public record GoalCompletionDto(
        GoalCompletionStrategy strategy

) {
}
