package com.finovara.corebackend.report.dto;

import com.finovara.activityservice.contracts.model.PeriodType;

import java.math.BigDecimal;

public record ReportDto(
        PeriodType periodType,
        BigDecimal amount
) {
}
