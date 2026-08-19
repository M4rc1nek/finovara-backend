package com.finovara.contracts.event.notification.limit;

import com.finovara.contracts.model.PeriodType;

import java.math.BigDecimal;

public record LimitStatsEvent(
        Long userId,
        Long limitId,
        BigDecimal percentage,
        PeriodType periodType
) {
}
