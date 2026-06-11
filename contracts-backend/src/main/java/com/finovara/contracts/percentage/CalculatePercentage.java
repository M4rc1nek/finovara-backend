package com.finovara.contracts.percentage;

import lombok.experimental.UtilityClass;

import java.math.BigDecimal;
import java.math.RoundingMode;

@UtilityClass
public class CalculatePercentage {

    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);
    private static final int DEFAULT_SCALE = 2;

    public static BigDecimal calculatePercentage(BigDecimal part, BigDecimal total) {
        return executePercentageCalculation(part, total, DEFAULT_SCALE);
    }

    public static BigDecimal calculateValueFromPercentage(BigDecimal amount, BigDecimal percentage) {
        return executeValueFromPercentageCalculation(amount, percentage, DEFAULT_SCALE);
    }

    private static BigDecimal executeValueFromPercentageCalculation(BigDecimal amount, BigDecimal percentage, int scale) {
        if (amount == null || percentage == null) {
            return BigDecimal.ZERO;
        }

        return amount
                .multiply(percentage)
                .divide(ONE_HUNDRED, scale, RoundingMode.HALF_UP);
    }

    private static BigDecimal executePercentageCalculation(BigDecimal part, BigDecimal total, int scale) {
        if (total == null || total.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal safePart = part != null ? part : BigDecimal.ZERO;

        return safePart
                .multiply(ONE_HUNDRED)
                .divide(total, scale, RoundingMode.HALF_UP);
    }
}
