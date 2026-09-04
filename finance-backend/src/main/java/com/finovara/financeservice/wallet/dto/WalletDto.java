package com.finovara.financeservice.wallet.dto;

import java.math.BigDecimal;

public record WalletDto(
        Long id,
        Long userId,
        BigDecimal balance,
        BigDecimal reservedAmount,
        BigDecimal availableAmount

) {
}