package com.finovara.finovarabackend.wallet.dto;

import java.math.BigDecimal;

public record WalletDto(
        Long id,
        Long userId,

        BigDecimal balance
) {
}
