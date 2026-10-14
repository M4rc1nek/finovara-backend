package com.finovara.contracts.mainaccount.report.financial.dto;

import com.finovara.contracts.util.PeriodType;

import java.math.BigDecimal;

public record ReportDto(
        PeriodType periodType,
        BigDecimal amount
) {
}
