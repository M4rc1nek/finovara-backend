package com.finovara.authbackend.usersetting.piggybank.completion.dto;

import com.finovara.authbackend.usersetting.piggybank.completion.model.GoalCompletionStrategy;

public record GoalCompletionDto(
        GoalCompletionStrategy strategy

) {
}
