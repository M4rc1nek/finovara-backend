package com.finovara.finovarabackend.report.finances.dto;

import java.math.BigDecimal;

public record ReportsAverageDTO(
        BigDecimal averageRevenue,
        BigDecimal averageExpense
) {
}
