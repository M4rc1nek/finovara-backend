package com.finovara.corebackend.wallet.dto;

import java.math.BigDecimal;

public record WalletDto(
        Long id,
        Long userId,

        BigDecimal balance
) {
}
