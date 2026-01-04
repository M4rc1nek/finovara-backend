package com.finovara.finovarabackend.reports.dto;

import java.math.BigDecimal;

public record ReportsSumDTO(
    BigDecimal sumRevenue,
    BigDecimal sumExpense
) {
}
