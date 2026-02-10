package com.finovara.finovarabackend.usersettings.finances.expense.savesettings.dto;

import com.finovara.finovarabackend.usersettings.finances.expense.controlamount.dto.ControlAmountDto;
import com.finovara.finovarabackend.usersettings.finances.expense.countlimit.dto.CountQuantityLimitDto;
import com.finovara.finovarabackend.usersettings.finances.expense.smartscan.dto.SmartScanDto;

public record SaveExpenseSettingsDto(
        ControlAmountDto controlAmountDto,
        CountQuantityLimitDto countQuantityLimitDto,
        SmartScanDto smartScanDto
) {
}
