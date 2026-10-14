package com.finovara.reportservice.util.dto.financial;

import java.math.BigDecimal;

public record FinancialTotalsDto(
        BigDecimal expenses,
        BigDecimal revenues,
        BigDecimal balance
) {
}

