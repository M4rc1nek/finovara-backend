package com.finovara.contracts.mainaccount.activity.event.wallet;

import com.finovara.contracts.mainaccount.activity.model.TransactionType;
import com.finovara.contracts.mainaccount.activity.model.WalletTransactionReason;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record WalletStatusActivityEvent(
        Long userId,
        TransactionType type,
        WalletTransactionReason reason,
        BigDecimal amount,
        LocalDateTime occurredAt
) {
}
