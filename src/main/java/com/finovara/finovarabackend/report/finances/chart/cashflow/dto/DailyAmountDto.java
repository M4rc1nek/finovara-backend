package com.finovara.finovarabackend.report.finances.chart.cashflow.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DailyAmountDto(
        LocalDate date,
        BigDecimal amount
) {
}
