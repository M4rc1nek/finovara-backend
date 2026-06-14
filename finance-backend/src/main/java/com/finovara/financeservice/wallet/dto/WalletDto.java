package com.finovara.authbackend.wallet.dto;

import java.math.BigDecimal;

public record WalletDto(
        Long id,
        Long userId,

        BigDecimal balance
) {
}
