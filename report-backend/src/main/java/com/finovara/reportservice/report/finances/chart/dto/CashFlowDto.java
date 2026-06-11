package com.finovara.reportservice.report.finances.chart.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CashFlowDto(
        BigDecimal revenue,
        BigDecimal expense,
        LocalDate date
        ) {
}
