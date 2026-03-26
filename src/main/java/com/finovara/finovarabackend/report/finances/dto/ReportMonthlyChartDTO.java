package com.finovara.finovarabackend.report.finances.dto;

import java.math.BigDecimal;

public record ReportMonthlyChartDTO(
        int day,
        BigDecimal income,
        BigDecimal expense
) {
}
