package com.finovara.finovarabackend.reports.finances.dto;

import java.math.BigDecimal;

public record ReportMonthlyChartDTO(
        int day,
        BigDecimal income,
        BigDecimal expense
) {
}
