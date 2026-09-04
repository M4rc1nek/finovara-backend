package com.finovara.financeservice.wallet.reservation.dto;

import java.math.BigDecimal;

public record UnreserveReservationDto(
        BigDecimal amount
) {
}
