package com.finovara.reportservice.savingsplan.dto.request;

import java.math.BigDecimal;

public enum SavingsStrategy {
    SAFE("0.6"),
    BALANCED("0.8"),
    AGGRESSIVE("1.0");

    private final BigDecimal rate;

    SavingsStrategy(String rate) {
        this.rate = new BigDecimal(rate);
    }

    public BigDecimal rate() {
        return rate;
    }
}
