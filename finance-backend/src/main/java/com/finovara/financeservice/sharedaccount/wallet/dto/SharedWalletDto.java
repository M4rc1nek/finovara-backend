package com.finovara.financeservice.sharedaccount.dto.wallet;

import java.math.BigDecimal;

public record SharedWalletDto(
        Long id,
        Long ownerId,
        Long memberId,

        BigDecimal balance
) {
}

