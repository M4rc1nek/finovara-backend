package com.finovara.finovarabackend.report.finances.chart.cashflow.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TotalCashFlowDto(
        LocalDate date,
        BigDecimal income,
        BigDecimal expense
) {
}
