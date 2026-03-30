package com.finovara.finovarabackend.report.finances.chart.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DateAmountDto(
        LocalDate date,
        BigDecimal amount
) {
}
