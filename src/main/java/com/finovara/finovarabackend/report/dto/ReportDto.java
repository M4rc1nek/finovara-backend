package com.finovara.finovarabackend.report.dto;

import com.finovara.finovarabackend.util.model.PeriodType;

import java.math.BigDecimal;

public record ReportDto(
        PeriodType periodType,
        BigDecimal amount
) {
}
