package com.finovara.activitylogservice.activitylog.mainaccount.wallet.dto;

import com.finovara.contracts.mainaccount.activity.model.TransactionType;
import com.finovara.contracts.mainaccount.activity.model.WalletTransactionReason;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record WalletStatusActivityDto(
        TransactionType type,
        WalletTransactionReason reason,
        BigDecimal amount,
        LocalDateTime createdAt
) {
}
