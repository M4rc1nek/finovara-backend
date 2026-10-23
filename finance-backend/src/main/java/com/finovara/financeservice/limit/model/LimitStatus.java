package com.finovara.financeservice.limit.model;

import java.math.BigDecimal;

public enum LimitStatus {
    NONE,
    LOW,
    MEDIUM,
    HIGH;

    private static final BigDecimal HIGH_THRESHOLD = BigDecimal.valueOf(80);
    private static final BigDecimal MEDIUM_THRESHOLD = BigDecimal.valueOf(50);
    private static final BigDecimal LOW_THRESHOLD = BigDecimal.valueOf(25);

    public static LimitStatus fromPercentage(BigDecimal percentage) {
        if (percentage.compareTo(HIGH_THRESHOLD) >= 0) return HIGH;
        if (percentage.compareTo(MEDIUM_THRESHOLD) >= 0) return MEDIUM;
        if (percentage.compareTo(LOW_THRESHOLD) >= 0) return LOW;
        return NONE;
    }
}