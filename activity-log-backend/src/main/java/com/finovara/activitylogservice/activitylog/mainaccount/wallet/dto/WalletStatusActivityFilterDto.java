package com.finovara.activitylogservice.activitylog.mainaccount.wallet.dto;

import com.finovara.contracts.mainaccount.activity.model.TransactionType;
import com.finovara.contracts.mainaccount.activity.model.WalletTransactionReason;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

public record WalletStatusActivityFilterDto(
        TransactionType type,
        WalletTransactionReason reason,
        BigDecimal amount,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdAt
) {
}
