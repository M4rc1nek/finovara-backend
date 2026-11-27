package com.finovara.reportservice.savingsplan.util;

import lombok.experimental.UtilityClass;

import java.math.BigDecimal;
import java.math.RoundingMode;

@UtilityClass
public final class MoneyUtils {

    public static final BigDecimal ZERO_MONEY = new BigDecimal("0.00");

    public static BigDecimal round(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP);
    }
}
