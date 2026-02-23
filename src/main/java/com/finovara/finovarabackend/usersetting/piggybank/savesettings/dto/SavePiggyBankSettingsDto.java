package com.finovara.finovarabackend.usersetting.piggybank.savesettings.dto;

import com.finovara.finovarabackend.usersetting.piggybank.autopayments.dto.AutoPaymentsDto;
import com.finovara.finovarabackend.usersetting.piggybank.completion.dto.GoalCompletionDto;
import com.finovara.finovarabackend.usersetting.piggybank.roundup.dto.RoundUpDto;

public record SavePiggyBankSettingsDto(
        AutoPaymentsDto autoPaymentsDto,
        RoundUpDto roundUpDto,
        GoalCompletionDto goalCompletionDto
) {
}
