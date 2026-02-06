package com.finovara.finovarabackend.usersettings.piggybank.savesettings.dto;

import com.finovara.finovarabackend.usersettings.piggybank.autopayments.dto.AutoPaymentsDto;
import com.finovara.finovarabackend.usersettings.piggybank.completion.dto.GoalCompletionDto;
import com.finovara.finovarabackend.usersettings.piggybank.roundup.dto.RoundUpDto;

public record SavePiggyBankSettingsDto(
        AutoPaymentsDto autoPaymentsDto,
        RoundUpDto roundUpDto,
        GoalCompletionDto goalCompletionDto
) {
}
