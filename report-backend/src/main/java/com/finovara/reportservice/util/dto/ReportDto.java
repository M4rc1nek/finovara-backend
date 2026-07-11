package com.finovara.reportservice.dto;

import com.finovara.contracts.model.PeriodType;

import java.math.BigDecimal;

public record ReportDto(
        PeriodType periodType,
        BigDecimal amount
) {
}
