package com.finovara.contracts.mainaccount.report.financial.dto;

import java.math.BigDecimal;

public record FinancialTotalsDto(
        BigDecimal expenses,
        BigDecimal revenues,
        BigDecimal balance
) {
}

