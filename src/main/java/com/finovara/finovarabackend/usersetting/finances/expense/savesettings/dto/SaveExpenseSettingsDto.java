package com.finovara.finovarabackend.usersetting.finances.expense.savesettings.dto;

import com.finovara.finovarabackend.usersetting.finances.expense.controlamount.dto.ControlAmountDto;
import com.finovara.finovarabackend.usersetting.finances.expense.countlimit.dto.CountQuantityLimitDto;
import com.finovara.finovarabackend.usersetting.finances.expense.smartscan.dto.SmartScanDto;

public record SaveExpenseSettingsDto(
        ControlAmountDto controlAmountDto,
        CountQuantityLimitDto countQuantityLimitDto,
        SmartScanDto smartScanDto
) {
}
