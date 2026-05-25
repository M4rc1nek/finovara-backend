package com.finovara.corebackend.usersetting.piggybank.completion.dto;

import com.finovara.corebackend.usersetting.piggybank.completion.model.GoalCompletionStrategy;

public record GoalCompletionDto(
        GoalCompletionStrategy strategy

) {
}
