package com.finovara.contracts.mainaccount.report.financial.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DailyCashDto(
        LocalDate date,
        BigDecimal amount
) {
}
