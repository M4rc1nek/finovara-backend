package com.finovara.reportservice.sharedaccount.report.finances.calculate.chart.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SharedCashFlowDto(
        BigDecimal revenue,
        BigDecimal expense,
        LocalDate date
        ) {
}
