package com.finovara.reportservice.util.dto.financial;

import java.math.BigDecimal;
import java.time.LocalDate;

public record FinancialCashFlowDto(
        BigDecimal revenue,
        BigDecimal expense,
        LocalDate date
) {
}

