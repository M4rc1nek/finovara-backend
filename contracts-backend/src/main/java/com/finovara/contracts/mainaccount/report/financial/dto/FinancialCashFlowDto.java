package com.finovara.contracts.mainaccount.report.financial.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record FinancialCashFlowDto(
        BigDecimal revenue,
        BigDecimal expense,
        LocalDate date
) {
}

