package com.finovara.finovarabackend.usersettings.piggybank.completion.dto;

import com.finovara.finovarabackend.usersettings.piggybank.completion.model.GoalCompletionStrategy;

public record GoalCompletionDto(
        Long piggyBankId,
        GoalCompletionStrategy strategy

) {
}
