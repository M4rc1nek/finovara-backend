package com.finovara.contracts.notification.event.limit;

import com.finovara.contracts.model.PeriodType;

import java.math.BigDecimal;

public record LimitStatsEvent(
        Long userId,
        Long limitId,
        BigDecimal percentage,
        PeriodType periodType
) {
}
