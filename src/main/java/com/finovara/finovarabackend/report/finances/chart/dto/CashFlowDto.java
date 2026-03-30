package com.finovara.finovarabackend.report.finances.chart.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CashFlowDto(
        LocalDate date,
        BigDecimal revenue,
        BigDecimal expense
) {
}
