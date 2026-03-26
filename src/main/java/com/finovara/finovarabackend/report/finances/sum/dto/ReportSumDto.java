package com.finovara.finovarabackend.report.finances.sum.dto;

import com.finovara.finovarabackend.report.finances.sum.model.ReportSumType;

import java.math.BigDecimal;

public record ReportSumDto(
        ReportSumType reportSumType,
        BigDecimal amount
) {
}
