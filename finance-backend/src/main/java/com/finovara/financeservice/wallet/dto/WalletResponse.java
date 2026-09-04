package com.finovara.financeservice.wallet.dto;

import com.finovara.financeservice.wallet.reservation.dto.FundReservationDto;

import java.math.BigDecimal;
import java.util.List;

public record WalletResponse(
        Long id,
        Long userId,
        BigDecimal balance,
        BigDecimal reservedAmount,
        BigDecimal availableAmount,
        List<FundReservationDto> reservations
) {
}