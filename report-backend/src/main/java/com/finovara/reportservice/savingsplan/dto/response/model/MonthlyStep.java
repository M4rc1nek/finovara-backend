package com.finovara.reportservice.savingsplan.dto.response.model;

import java.math.BigDecimal;

public record MonthlyStep(int month, BigDecimal expectedSavings) {
}
