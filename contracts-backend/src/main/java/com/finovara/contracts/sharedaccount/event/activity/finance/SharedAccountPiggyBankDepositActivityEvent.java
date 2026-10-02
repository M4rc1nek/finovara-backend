package com.finovara.contracts.sharedaccount.event.activity.finance;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SharedAccountPiggyBankDepositActivityEvent(
        Long ownerId,
        Long memberId,
        Long userId,
        Long piggyBankId,
        BigDecimal amount,
        LocalDateTime createdAt) {}