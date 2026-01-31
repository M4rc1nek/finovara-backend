package com.finovara.finovarabackend.usersettings.piggybank.savesettings.dto;

import com.finovara.finovarabackend.usersettings.piggybank.autopayments.dto.AutoPaymentsDto;
import com.finovara.finovarabackend.usersettings.piggybank.completion.dto.GoalCompletionDto;
import com.finovara.finovarabackend.usersettings.piggybank.roundup.dto.RoundUpDto;

import java.util.List;

public record SavePiggyBankSettingsDto(
        List<AutoPaymentsDto> autoPaymentsDto,
        List<RoundUpDto> roundUpDto,
        List<GoalCompletionDto> goalCompletionDto
) {
}
