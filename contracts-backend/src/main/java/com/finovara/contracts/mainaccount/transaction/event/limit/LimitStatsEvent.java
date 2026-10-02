package com.finovara.contracts.mainaccount.transaction.event.limit;

import com.finovara.contracts.util.PeriodType;

import java.math.BigDecimal;

public record LimitStatsEvent(
        Long userId,
        Long limitId,
        BigDecimal percentage,
        PeriodType periodType
) {
}
