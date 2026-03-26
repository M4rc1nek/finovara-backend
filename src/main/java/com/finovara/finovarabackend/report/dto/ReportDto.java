package com.finovara.finovarabackend.report.dto;

import com.finovara.finovarabackend.report.model.ReportPeriodType;

import java.math.BigDecimal;

public record ReportDto(
        ReportPeriodType reportPeriodType,
        BigDecimal amount
) {
}
