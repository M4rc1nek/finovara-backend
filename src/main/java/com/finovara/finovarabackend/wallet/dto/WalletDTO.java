package com.finovara.finovarabackend.wallet.dto;

import java.math.BigDecimal;

public record WalletDTO(
        Long id,
        Long userId,

        BigDecimal balance
) {
}
