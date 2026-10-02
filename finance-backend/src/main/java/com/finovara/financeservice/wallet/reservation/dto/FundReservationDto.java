package com.finovara.financeservice.wallet.reservation.dto;

import com.finovara.contracts.util.model.ExpenseCategory;

import java.math.BigDecimal;

public record FundReservationDto(
        Long id,
        ExpenseCategory category,
        BigDecimal amount,
        String authorizationCode
) {
}
