package com.finovara.financeservice.sharedaccount.wallet.dto;

import java.math.BigDecimal;

public record SharedWalletDto(
        Long id,
        Long ownerId,
        Long memberId,

        BigDecimal balance
) {
}

