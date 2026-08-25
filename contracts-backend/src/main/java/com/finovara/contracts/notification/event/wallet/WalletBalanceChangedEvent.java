package com.finovara.contracts.notification.event.wallet;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record WalletBalanceChangedEvent(
        Long userId,
        BigDecimal previousBalance,
        BigDecimal currentBalance,
        LocalDateTime changedAt
) {
}