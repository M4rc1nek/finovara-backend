package com.finovara.financeservice.limit.dto;

import java.math.BigDecimal;

public record LimitSpendingDto(Long limitId, BigDecimal amount) {
}
