package com.finovara.finovarabackend.reports.dto;

import java.math.BigDecimal;

public record ReportsAverageDTO(
        BigDecimal averageRevenue,
        BigDecimal averageExpense
) {
}
