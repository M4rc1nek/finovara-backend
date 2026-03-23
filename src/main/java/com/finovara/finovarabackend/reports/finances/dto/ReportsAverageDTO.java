package com.finovara.finovarabackend.reports.finances.dto;

import java.math.BigDecimal;

public record ReportsAverageDTO(
        BigDecimal averageRevenue,
        BigDecimal averageExpense
) {
}
