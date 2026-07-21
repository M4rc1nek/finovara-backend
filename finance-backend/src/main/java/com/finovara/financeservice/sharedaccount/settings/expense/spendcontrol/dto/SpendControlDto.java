package com.finovara.financeservice.sharedaccount.settings.spendcontrol.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;

import java.math.BigDecimal;

public record SpendControlDto(
        Boolean spendControlEnabled,

        @DecimalMin("1") @DecimalMax("100") BigDecimal spendControlPercentage
) {
}
