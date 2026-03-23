package com.finovara.finovarabackend.reports.finances.dto;

import java.math.BigDecimal;

public record ReportsSumDTO(
    BigDecimal sumRevenue,
    BigDecimal sumExpense
) {
}
